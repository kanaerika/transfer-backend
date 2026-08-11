package com.afb.transferplatform.service;

import com.afb.transferplatform.dto.AuthDtos.*;
import com.afb.transferplatform.entity.Agent;
import com.afb.transferplatform.entity.Partenaire;
import com.afb.transferplatform.repository.AgentRepository;
import com.afb.transferplatform.repository.PartenaireRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Réservé à l'ADMIN du partenaire Afriland : gestion des partenaires et de leurs administrateurs. */
@Service
public class SuperAdminService {

    private final PartenaireRepository partenaires;
    private final AgentRepository agents;
    private final InvitationService invitations;
    private static final String ROLE_ADMIN = "ADMIN";

    public SuperAdminService(PartenaireRepository partenaires, AgentRepository agents,
                             InvitationService invitations) {
        this.partenaires = partenaires;
        this.agents = agents;
        this.invitations = invitations;
    }

    public List<PartenaireResponse> lister(Agent admin) {
        verifierAfriland(admin);
        return partenaires.findAll().stream().map(this::versReponse).toList();
    }

    public PartenaireResponse detail(Long id, Agent admin) {
        verifierAfriland(admin);
        return versReponse(charger(id));
    }

    /** Crée le partenaire ET son administrateur, puis envoie l'invitation par email. */
    @Transactional
    public PartenaireResponse creer(PartenaireRequest req, Agent appelant) {
        verifierAfriland(appelant);
        String nom = req.nom().trim();
        String email = req.email().trim().toLowerCase();
        if (partenaires.existsByNomIgnoreCase(nom)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce partenaire existe déjà.");
        }
        if (agents.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un compte existe déjà avec cet email.");
        }
        Partenaire p = partenaires.save(new Partenaire(nom, email));

        Agent admin = new Agent();
        admin.setNomComplet(req.nomAdministrateur().trim());
        admin.setEmail(email);
        admin.setRole(ROLE_ADMIN);
        admin.setPartenaire(p);
        invitations.inviter(admin);
        agents.save(admin);
        return versReponse(p);
    }

    @Transactional
    public PartenaireResponse modifier(Long id, ModificationPartenaireRequest req, Agent admin) {
        verifierAfriland(admin);
        Partenaire p = charger(id);
        p.setNom(req.nom().trim());
        p.setEmail(req.email().trim().toLowerCase());
        partenaires.save(p);
        return versReponse(p);
    }

    /** Active ou désactive le partenaire (ses utilisateurs ne peuvent plus se connecter). */
    @Transactional
    public PartenaireResponse basculerActif(Long id, Agent admin) {
        verifierAfriland(admin);
        Partenaire p = charger(id);
        p.setActif(!p.isActif());
        partenaires.save(p);
        return versReponse(p);
    }

    /**
     * Supprime un partenaire — uniquement s'il n'a plus aucun agent rattaché.
     * Un partenaire avec des agents (donc potentiellement des transferts) doit être
     * désactivé plutôt que supprimé, pour préserver l'historique.
     */
    @Transactional
    public void supprimer(Long id, Agent admin) {
        verifierAfriland(admin);
        Partenaire p = charger(id);
        if (!agents.findByPartenaireIdOrderByNomComplet(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce partenaire a des agents rattachés : désactivez-le plutôt que de le supprimer.");
        }
        partenaires.delete(p);
    }

    /** Renvoie l'invitation à l'administrateur du partenaire. */
    @Transactional
    public MessageResponse reinviter(Long id, Agent admin) {
        verifierAfriland(admin);
        Partenaire p = charger(id);
        Agent adminCible = agents.findFirstByPartenaireIdAndRole(p.getId(), ROLE_ADMIN)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Administrateur du partenaire introuvable."));
        if (adminCible.isInvitationAcceptee() && !adminCible.isFirstLogin()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce compte est déjà activé.");
        }
        invitations.inviter(adminCible);
        agents.save(adminCible);
        return new MessageResponse("Nouvelle invitation envoyée à " + adminCible.getEmail() + ".");
    }

    /** Réservé à l'admin du partenaire fondateur (Afriland) : lui seul onboarde de nouveaux partenaires. */
    private void verifierAfriland(Agent admin) {
        if (admin == null || !admin.estAdminAfriland()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Réservé à l'administrateur Afriland.");
        }
    }

    private Partenaire charger(Long id) {
        return partenaires.findById(Objects.requireNonNull(id)).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Partenaire introuvable."));
    }

    private PartenaireResponse versReponse(Partenaire p) {
        String statut = agents.findFirstByPartenaireIdAndRole(p.getId(), ROLE_ADMIN)
                .map(a -> {
                    if (!a.isInvitationAcceptee()) return "Invitation en attente";
                    if (a.isFirstLogin()) return "Mot de passe temporaire";
                    return "Administrateur actif";
                })
                .orElse("Aucun administrateur");
        return new PartenaireResponse(p.getId(), p.getNom(), p.getEmail(), p.isActif(), statut);
    }
}
