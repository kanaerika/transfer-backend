package com.afb.transferplatform.service;

import com.afb.transferplatform.dto.StatistiquesDtos.*;
import com.afb.transferplatform.entity.*;
import com.afb.transferplatform.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Statistiques adaptées au rôle :
 *  - ADMIN → uniquement son organisation (Afriland y compris : jamais toute la plateforme)
 *  - AGENT → uniquement ses propres transferts
 */
@Service
public class StatistiquesService {

    private static final String ROLE_AGENT = "AGENT";

    // Couleurs alignées sur le thème (utilisées côté donut).
    private static final String C_VERT   = "#128C4A";
    private static final String C_ROUGE  = "#C8102E";
    private static final String C_ORANGE = "#E08A00";
    private static final String C_BLEU   = "#1F6FEB";

    private final TransfertRepository transferts;
    private final AgentRepository agents;
    private final PartenaireRepository partenaires;

    public StatistiquesService(TransfertRepository transferts,
                               AgentRepository agents,
                               PartenaireRepository partenaires) {
        this.transferts = transferts;
        this.agents = agents;
        this.partenaires = partenaires;
    }
    // ---------- SUPER ADMIN : plateforme ----------

    /** Transactionnel : l'activité récente traverse des associations LAZY (Transfert.agent). */
    @Transactional(readOnly = true)
    public StatistiquesResponse pour(Agent utilisateur) {
        return switch (utilisateur.getRole()) {
            case "ADMIN" -> partenaire(utilisateur);
            case "AGENT" -> agent(utilisateur);
            default -> throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rôle inconnu.");
        };
    }

    // ---------- ADMIN PARTENAIRE : son organisation ----------

    private StatistiquesResponse partenaire(Agent admin) {
        Long pid = partenaireDe(admin);

        List<Agent> mesAgents = agents.findByPartenaireIdOrderByNomComplet(pid).stream()
                .filter(a -> ROLE_AGENT.equals(a.getRole())).toList();
        long total = mesAgents.size();
        long actifs = mesAgents.stream().filter(Agent::isActif).count();
        long enAttente = mesAgents.stream().filter(a -> !a.isInvitationAcceptee()).count();
        long transfertsPartenaire = transferts.countByPartenaireId(pid);

        List<Kpi> kpis = List.of(
                new Kpi("Agents", total, "principal"),
                new Kpi("Agents actifs", actifs, "succes"),
                new Kpi("Agents désactivés", total - actifs, "neutre"),
                new Kpi("Invitations en attente", enAttente, "attention"),
                new Kpi("Transferts", transfertsPartenaire, "principal")
        );

        return new StatistiquesResponse(
                "PARTENAIRE",
                kpis,
                repartition(transferts.repartitionParStatutPartenaire(pid)),
                evolution(transferts.transfertsParMoisPartenaire(pid, douzeMoisAvant())),
                classement(transferts.topAgents(pid, PageRequest.of(0, 5))),
                activite(transferts.findTop8ByPartenaireIdOrderByIdDesc(pid))
        );
    }

    // ---------- AGENT : lui-même ----------

    private StatistiquesResponse agent(Agent agent) {
        Long aid = agent.getId();
        LocalDate aujourdhui = LocalDate.now();

        long jour = transferts.countByAgentIdAndDateTransfertBetween(aid, aujourdhui, aujourdhui);
        long semaine = transferts.countByAgentIdAndDateTransfertBetween(
                aid, aujourdhui.minusDays(6), aujourdhui);
        long mois = transferts.countByAgentIdAndDateTransfertBetween(
                aid, aujourdhui.withDayOfMonth(1), aujourdhui);
        long executes = transferts.countByAgentIdAndStatut(aid, StatutTransfert.EXECUTE);

        List<Kpi> kpis = List.of(
                new Kpi("Aujourd'hui", jour, "principal"),
                new Kpi("Cette semaine", semaine, "info"),
                new Kpi("Ce mois", mois, "succes"),
                new Kpi("Total exécutés", executes, "principal")
        );

        return new StatistiquesResponse(
                "AGENT",
                kpis,
                repartition(transferts.repartitionParStatutAgent(aid)),
                evolution(transferts.transfertsParMoisAgent(aid, douzeMoisAvant())),
                List.of(),
                activite(transferts.findTop8ByAgentIdOrderByIdDesc(aid))
        );
    }

    // ---------- Helpers ----------

    private Long partenaireDe(Agent admin) {
        if (admin.getPartenaire() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Compte non rattaché à un partenaire.");
        }
        return admin.getPartenaire().getId();
    }

    private LocalDate douzeMoisAvant() {
        return LocalDate.now().minusMonths(11).withDayOfMonth(1);
    }

    /** Transforme [statut, count] en parts colorées pour le donut. */
    private List<Part> repartition(List<Object[]> lignes) {
        Map<StatutTransfert, Long> map = new EnumMap<>(StatutTransfert.class);
        for (Object[] l : lignes) {
            map.put((StatutTransfert) l[0], ((Number) l[1]).longValue());
        }
        List<Part> parts = new ArrayList<>();
        parts.add(new Part("Exécutés", map.getOrDefault(StatutTransfert.EXECUTE, 0L), C_VERT));
        parts.add(new Part("En cours", map.getOrDefault(StatutTransfert.EN_COURS, 0L), C_BLEU));
        parts.add(new Part("Annulés", map.getOrDefault(StatutTransfert.ANNULE, 0L), C_ORANGE));
        parts.add(new Part("Rejetés", map.getOrDefault(StatutTransfert.REJETE, 0L), C_ROUGE));
        return parts;
    }

    /** Transforme [année, mois, count] en série continue sur 12 mois (les mois vides = 0). */
    private List<PointTemporel> evolution(List<Object[]> lignes) {
        Map<String, Long> valeurs = new HashMap<>();
        for (Object[] l : lignes) {
            int annee = ((Number) l[0]).intValue();
            int mois = ((Number) l[1]).intValue();
            valeurs.put(annee + "-" + mois, ((Number) l[2]).longValue());
        }
        List<PointTemporel> serie = new ArrayList<>();
        LocalDate curseur = douzeMoisAvant();
        for (int i = 0; i < 12; i++) {
            String cle = curseur.getYear() + "-" + curseur.getMonthValue();
            String libelle = curseur.getMonth()
                    .getDisplayName(TextStyle.SHORT, Locale.FRENCH);
            serie.add(new PointTemporel(libelle, valeurs.getOrDefault(cle, 0L)));
            curseur = curseur.plusMonths(1);
        }
        return serie;
    }

    private List<Classement> classement(List<Object[]> lignes) {
        List<Classement> res = new ArrayList<>();
        for (Object[] l : lignes) {
            res.add(new Classement(
                    ((Number) l[0]).longValue(),
                    (String) l[1],
                    ((Number) l[2]).longValue()));
        }
        return res;
    }

    private List<TransfertResume> activite(List<Transfert> liste) {
        return liste.stream().map(t -> new TransfertResume(
                t.getId(),
                t.getNomClient(),
                t.getMontant(),
                t.getStatut().name(),
                t.getDateTransfert().toString(),
                t.getAgent() != null ? t.getAgent().getNomComplet() : "—"
        )).toList();
    }
}