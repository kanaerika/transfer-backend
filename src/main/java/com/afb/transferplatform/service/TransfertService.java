package com.afb.transferplatform.service;


import com.afb.transferplatform.dto.TransfertDtos.*;
import com.afb.transferplatform.entity.Agent;
import com.afb.transferplatform.entity.CompteurJournalier;
import com.afb.transferplatform.entity.StatutTransfert;
import com.afb.transferplatform.entity.Transfert;
import com.afb.transferplatform.repository.CompteurJournalierRepository;
import com.afb.transferplatform.repository.TransfertRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class TransfertService {

    private final TransfertRepository transfertRepository;
    private final CompteurJournalierRepository compteurRepository;
    private final ConfigurationService configuration;


    private static final DateTimeFormatter FMT_FR =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);

    /** Alphabet sans caractères ambigus (0/O, 1/I) : majoritairement des lettres pour une plage large. */
    private static final String ALPHABET_CODE = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();

    /** Génère une référence de vérification aléatoire (ex: V4K9QXHB), non séquentielle. */
    private static String genererReferenceVerification(String prefixe) {
        StringBuilder code = new StringBuilder(prefixe);
        for (int i = 0; i < 8; i++) {
            code.append(ALPHABET_CODE.charAt(RANDOM.nextInt(ALPHABET_CODE.length())));
        }
        return code.toString();
    }

    /** Normalise un nom : espaces superflus supprimés (début, fin, doublons). */
    private static String normaliser(String nom) {
        return nom == null ? "" : nom.trim().replaceAll("\\s{2,}", " ");
    }

    /** Vérifie la cohérence du n° de pièce selon sa nature. */
    private static void validerPiece(String nature, String numero) {
        String n = numero == null ? "" : numero.trim();
        if (!n.matches("[A-Za-z0-9]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le n° de pièce ne doit contenir que des lettres et des chiffres (sans espaces).");
        }

        if ("Carte Nationale d'Identité".equals(nature)) {
            // Nouvelles cartes : 2 lettres + 8 chiffres (ex. AB12345678)
            // Anciennes cartes : 18 chiffres
            boolean nouvelleCarte = n.matches("[A-Za-z]{2}\\d{8}");
            boolean ancienneCarte = n.matches("\\d{18}");
            if (!nouvelleCarte && !ancienneCarte) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "N° de CNI invalide : attendu 18 chiffres (ancienne carte) "
                        + "ou 2 lettres suivies de 8 chiffres (nouvelle carte).");
            }
            return;
        }

        int min = switch (nature == null ? "" : nature) {
            case "Passeport" -> 7;
            default -> 6;
        };
        if (n.length() < min) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "N° de pièce trop court pour une pièce de type « " + nature
                            + " » (minimum " + min + " caractères).");
        }
    }

    public TransfertService(TransfertRepository transfertRepository,
                            CompteurJournalierRepository compteurRepository,
                            ConfigurationService configuration) {
        this.transfertRepository = transfertRepository;
        this.compteurRepository = compteurRepository;
        this.configuration = configuration;
    }

    /** Vérifie si le client peut transférer sans dépasser le plafond mensuel global. */
    @Transactional
    public VerificationResponse verifier(VerificationRequest req, Agent agent) {
        String nomClient = normaliser(req.nomClient());
        validerPiece(req.naturePiece(), req.numeroPiece());

        long plafond = configuration.plafondMensuel();
        long cumul = cumulDuMois(nomClient);
        long restant = Math.max(0, plafond - cumul);
        boolean autorise = req.montant() <= restant;

        if (!autorise) {
            // Traçabilité : chaque refus est enregistré comme un transfert REFUSE_PLAFOND,
            // visible dans le bilan journalier et consultable en détail.
            String motif = String.format(Locale.FRENCH,
                    "Plafond dépassé : cumul %,d + montant %,d dépasse le plafond %,d FCFA.",
                    cumul, req.montant(), plafond);
            enregistrerRefus(req, nomClient, cumul, motif, agent);
            incrementer(agent, c -> c.setRejetes(c.getRejetes() + 1));
        }

        int pctUtilise = (int) Math.min(100, Math.round(cumul * 100.0 / plafond));
        int pctApres = (int) Math.min(100, Math.round((cumul + req.montant()) * 100.0 / plafond));

        DernierTransfert dernier = transfertRepository
                .findFirstByNomClientIgnoreCaseOrderByIdDesc(nomClient)
                .map(t -> new DernierTransfert(t.getNomClient(),
                        t.getDateTransfert().format(FMT_FR), t.getMontant(),
                        t.getStatut().getLibelle()))
                .orElse(null);

        String montantFmt = String.format(Locale.FRENCH, "%,d", req.montant());
        String message = autorise
                ? "Le transfert de " + montantFmt + " FCFA est valide. Vous pouvez exécuter cette opération."
                : "Plafond dépassé — ce client ne peut pas transférer " + montantFmt + " FCFA ce mois-ci.";

        return new VerificationResponse(autorise, message, plafond, cumul,
                req.montant(), restant, pctUtilise, pctApres, dernier);
    }

    /** Enregistre une tentative refusée pour dépassement de plafond (trace au bilan). */
    private void enregistrerRefus(VerificationRequest req, String nomClient,
                                  long cumul, String motif, Agent agent) {
        Transfert t = new Transfert();
        t.setNomClient(nomClient);
        t.setDateNaissance(req.dateNaissance().trim());
        t.setNaturePiece(req.naturePiece());
        t.setNumeroPiece(req.numeroPiece().trim());
        t.setMontant(req.montant());
        t.setPaysDestination(req.paysDestination());
        t.setStatut(StatutTransfert.REFUSE_PLAFOND);
        t.setMotif(motif);
        t.setAgence(agent.getAgence());
        t.setDateTransfert(LocalDate.now(java.time.Clock.systemDefaultZone()));
        t.setCumulMois(cumul); // le refus ne modifie pas le cumul
        t.setAgent(agent);
        t.setPartenaire(agent.getPartenaire());
        t.setReferenceVerification(genererReferenceVerification("R"));
        transfertRepository.save(t);
    }

    /** Enregistre le transfert exécuté (après saisie de la référence plateforme). */
    @Transactional
    public TransfertResponse executer(ExecutionRequest req, Agent agent) {
        String nomClient = normaliser(req.nomClient());
        validerPiece(req.naturePiece(), req.numeroPiece());

        long cumul = cumulDuMois(nomClient);
        if (req.montant() > configuration.plafondMensuel() - cumul) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Plafond mensuel dépassé : exécution refusée.");
        }

        Transfert t = new Transfert();
        t.setNomClient(nomClient);
        t.setDateNaissance(req.dateNaissance().trim());
        t.setNaturePiece(req.naturePiece());
        t.setNumeroPiece(req.numeroPiece().trim());
        t.setMontant(req.montant());
        t.setPaysDestination(req.paysDestination());
        t.setStatut(StatutTransfert.EXECUTE);
        t.setReference(req.reference().trim());
        t.setAgence(agent.getAgence());
        t.setCanal(req.canal());
        t.setDateTransfert(LocalDate.now(java.time.Clock.systemDefaultZone()));
        t.setCumulMois(cumul + req.montant());
        t.setAgent(agent);
        t.setPartenaire(agent.getPartenaire());
        t.setReferenceVerification(genererReferenceVerification("V"));
        transfertRepository.save(t);

        incrementer(agent, c -> c.setExecutes(c.getExecutes() + 1));
        return TransfertResponse.from(t);
    }

    /** Historique scopé au partenaire de l'agent connecté. */
    @Transactional(readOnly = true)
    public List<TransfertResponse> historique(String recherche, Agent agent) {
        return filtrer(transfertRepository.findByPartenaireIdOrderByIdDesc(partenaireDe(agent)), recherche);
    }

    /** Transferts exécutés (annulables) du partenaire de l'agent connecté. */
    @Transactional(readOnly = true)
    public List<TransfertResponse> annulables(String recherche, Agent agent) {
        return filtrer(transfertRepository.findByPartenaireIdAndStatutOrderByIdDesc(
                partenaireDe(agent), StatutTransfert.EXECUTE), recherche);
    }

    /** Détail d'un transfert (page « voir informations »), scopé au partenaire. */
    @Transactional(readOnly = true)
    public TransfertResponse detail(Long id, Agent agent) {
        Transfert t = transfertRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Transfert introuvable."));
        if (t.getPartenaire() == null
                || !t.getPartenaire().getId().equals(partenaireDe(agent))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Ce transfert n'appartient pas à votre institution.");
        }
        return TransfertResponse.from(t);
    }

    private static Long partenaireDe(Agent agent) {
        if (agent.getPartenaire() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Compte non rattaché à un partenaire.");
        }
        return agent.getPartenaire().getId();
    }

    @Transactional
    public TransfertResponse annuler(Long id, String motif, Agent agent) {
        validerMotif(motif);
        Transfert t = transfertRepository.findById(Objects.requireNonNull(id))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Transfert introuvable."));
        if (t.getStatut() != StatutTransfert.EXECUTE) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Seul un transfert exécuté peut être annulé.");
        }
        t.setStatut(StatutTransfert.ANNULE);
        t.setMotif(motif.trim());
        transfertRepository.save(t);
        incrementer(agent, c -> c.setAnnules(c.getAnnules() + 1));
        return TransfertResponse.from(t);
    }

    /** Rejet d'un transfert exécuté (avec motif obligatoire, min. 10 caractères). */
    @Transactional
    public TransfertResponse rejeter(Long id, String motif, Agent agent) {
        validerMotif(motif);
        Transfert t = transfertRepository.findById(Objects.requireNonNull(id))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Transfert introuvable."));
        if (t.getStatut() != StatutTransfert.EXECUTE) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Seul un transfert exécuté peut être rejeté.");
        }
        t.setStatut(StatutTransfert.REJETE);
        t.setMotif(motif.trim());
        transfertRepository.save(t);
        incrementer(agent, c -> c.setRejetes(c.getRejetes() + 1));
        return TransfertResponse.from(t);
    }

    private static void validerMotif(String motif) {
        if (motif == null || motif.trim().length() < 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le motif est obligatoire et doit contenir au moins 10 caractères.");
        }
    }

    /** Bilan du jour : totaux + liste détaillée de tous les transferts du jour. */
    @Transactional(readOnly = true)
    public BilanResponse bilan(Agent agent) {
        LocalDate jour = LocalDate.now(java.time.Clock.systemDefaultZone());
        CompteurJournalier c = compteurRepository.findByAgentAndJour(agent, jour)
            .orElseGet(() -> new CompteurJournalier(agent, jour));

        List<TransfertResponse> lignes = transfertRepository
                .findByAgentIdAndDateTransfertOrderByIdDesc(agent.getId(), jour)
                .stream().map(TransfertResponse::from).toList();

        int total = lignes.size();
        return new BilanResponse(jour, c.getExecutes(), c.getRejetes(),
            c.getAnnules(), c.getNonClotures(), total, lignes);
    }

    // ------------------------------------------------------------------

    private long cumulDuMois(String nomClient) {
        LocalDate maintenant = LocalDate.now(java.time.Clock.systemDefaultZone());
        LocalDate debut = maintenant.withDayOfMonth(1);
        LocalDate fin = maintenant.withDayOfMonth(maintenant.lengthOfMonth());
        return transfertRepository.cumulMensuel(normaliser(nomClient), debut, fin);
    }

    private List<TransfertResponse> filtrer(List<Transfert> liste, String recherche) {
        String q = recherche == null ? "" : recherche.trim().toLowerCase();
        return liste.stream()
                .filter(t -> q.isEmpty()
                        || (t.getReference() != null && t.getReference().toLowerCase().contains(q))
                        || t.getNomClient().toLowerCase().contains(q))
                .map(TransfertResponse::from)
                .toList();
    }

    private void incrementer(Agent agent, java.util.function.Consumer<CompteurJournalier> maj) {
        LocalDate jour = LocalDate.now(java.time.Clock.systemDefaultZone());
        CompteurJournalier c = compteurRepository.findByAgentAndJour(agent, jour)
            .orElseGet(() -> new CompteurJournalier(agent, jour));
        maj.accept(c);
        compteurRepository.save(Objects.requireNonNull(c));
    }

    /** Auto-complétion : clients connus dont le nom commence par la saisie (min. 2 caractères). */
    public List<ClientConnu> clientsConnus(String prefixe) {
        if (prefixe == null || prefixe.trim().length() < 2) return List.of();
        return transfertRepository.rechercherClientsConnus(normaliser(prefixe)).stream()
                .limit(6)
                .map(t -> new ClientConnu(
                        t.getNomClient(),
                        t.getDateNaissance(),
                        t.getNaturePiece(),
                        t.getNumeroPiece()))
                .toList();
    }
}