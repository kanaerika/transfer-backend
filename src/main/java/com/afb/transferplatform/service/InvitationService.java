package com.afb.transferplatform.service;

import com.afb.transferplatform.entity.Agent;
import com.afb.transferplatform.repository.AgentRepository;
import com.afb.transferplatform.utile.MotDePasseGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Gestion des invitations.
 *
 * Principe : le compte est créé avec un mot de passe TEMPORAIRE communiqué par email.
 * Le lien d'invitation sert uniquement à prouver la possession de l'adresse email ;
 * il ne permet pas de définir un mot de passe.
 */
@Service
public class InvitationService {

    private static final Duration VALIDITE = Duration.ofHours(72);

    private final EmailService emailService;
    private final AgentRepository agentRepository;
    private final PasswordEncoder passwordEncoder;
    private final String urlFrontend;

    public InvitationService(EmailService emailService,
                             AgentRepository agentRepository,
                             PasswordEncoder passwordEncoder,
                             @Value("${app.frontend-url:http://localhost:4200}") String urlFrontend) {
        this.emailService = emailService;
        this.agentRepository = agentRepository;
        this.passwordEncoder = passwordEncoder;
        this.urlFrontend = urlFrontend;
    }

    /**
     * (Ré)initialise l'invitation d'un compte :
     * nouveau mot de passe temporaire, nouveau token, ancien token invalidé, email envoyé.
     *
     * L'agent n'est pas sauvegardé ici : l'appelant reste maître de la transaction.
     */
    public void inviter(Agent agent) {
        String motDePasseTemporaire = MotDePasseGenerator.generer();

        agent.setMotDePasse(passwordEncoder.encode(motDePasseTemporaire));
        agent.setTokenInvitation(UUID.randomUUID().toString());
        agent.setTokenExpiration(Instant.now().plus(VALIDITE));
        agent.setInvitationAcceptee(false);
        agent.setFirstLogin(true);

        envoyerEmail(agent, motDePasseTemporaire);
    }

    /**
     * Validation du lien reçu par email.
     * Vérifie l'existence et la validité du token, marque l'invitation comme acceptée,
     * puis rend le token définitivement inutilisable.
     */
    @Transactional
    public Agent validerInvitation(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Lien d'invitation invalide.");
        }

        Agent agent = agentRepository.findByTokenInvitation(token.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Lien d'invitation invalide ou déjà utilisé."));

        if (agent.getTokenExpiration() == null
                || Instant.now().isAfter(agent.getTokenExpiration())) {
            throw new ResponseStatusException(HttpStatus.GONE,
                    "Lien d'invitation expiré. Demandez une nouvelle invitation.");
        }

        agent.setInvitationAcceptee(true);
        agent.setTokenInvitation(null);
        agent.setTokenExpiration(null);

        return agentRepository.save(agent);
    }

    private void envoyerEmail(Agent agent, String motDePasseTemporaire) {
        String lien = urlFrontend + "/invitation?token=" + agent.getTokenInvitation();
        String sujet = "Welcome to Afriland First Bank — Activate Your Account";

        String message = "AGENT".equals(agent.getRole())
                ? EmailTemplates.invitationAgent(
                        urlFrontend, lien, agent.getTokenExpiration(),
                        agent.getNomComplet(), agent.getNomPartenaire(), administrateurDe(agent),
                        agent.getEmail(), motDePasseTemporaire)
                : EmailTemplates.invitationPartenaireAdmin(
                        urlFrontend, lien, agent.getTokenExpiration(),
                        agent.getNomPartenaire(), agent.getNomComplet(),
                        agent.getEmail(), motDePasseTemporaire);

        emailService.envoyer(agent.getEmail(), sujet, message);
    }

    /** Nom de l'administrateur de l'organisation d'un agent, affiché dans son email d'invitation. */
    private String administrateurDe(Agent agent) {
        if (agent.getPartenaire() == null) return agent.getNomPartenaire();
        return agentRepository.findFirstByPartenaireIdAndRole(agent.getPartenaire().getId(), "ADMIN")
                .map(Agent::getNomComplet)
                .orElse(agent.getNomPartenaire());
    }
}