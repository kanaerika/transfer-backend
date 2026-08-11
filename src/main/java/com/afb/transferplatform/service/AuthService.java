package com.afb.transferplatform.service;

import com.afb.transferplatform.dto.AuthDtos.*;
import com.afb.transferplatform.entity.Agent;
import com.afb.transferplatform.repository.AgentRepository;
import com.afb.transferplatform.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Connexion, changement obligatoire à la première connexion, changement volontaire. */
@Service
public class AuthService {

    private final AgentRepository agentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AgentRepository agentRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.agentRepository = agentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest request) {
        Agent agent = agentRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(this::identifiantsInvalides);

        // Le mot de passe est vérifié en premier : on ne révèle pas l'état d'un compte
        // à quelqu'un qui n'en connaît pas le mot de passe.
        if (agent.getMotDePasse() == null
                || !passwordEncoder.matches(request.motDePasse(), agent.getMotDePasse())) {
            throw identifiantsInvalides();
        }

        if (!agent.isInvitationAcceptee()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Compte non activé. Cliquez sur le lien reçu par email pour valider votre invitation.");
        }
        if (!agent.isActif()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Compte désactivé. Contactez votre administrateur.");
        }
        if (agent.getPartenaire() != null && !agent.getPartenaire().isActif()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Le partenaire " + agent.getPartenaire().getNom() + " est désactivé.");
        }

        return new AuthResponse(
                jwtService.generer(agent.getId()),
                agent.getId(), agent.getNomComplet(), agent.getCodeAgent(),
                agent.getRole(), agent.getNomPartenaire(),
                agent.isFirstLogin());
    }

    /** Première connexion : remplacement du mot de passe temporaire, sans ancien mot de passe. */
    @Transactional
    public MessageResponse definirPremierMotDePasse(PremierMotDePasseRequest request, Agent agent) {
        if (!agent.isFirstLogin()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Votre mot de passe a déjà été défini. Utilisez « Mon profil » pour le modifier.");
        }
        verifierConcordance(request.nouveauMotDePasse(), request.confirmationMotDePasse());
        verifierPolitique(request.nouveauMotDePasse());

        agent.setMotDePasse(passwordEncoder.encode(request.nouveauMotDePasse()));
        agent.setFirstLogin(false);
        agentRepository.save(agent);

        return new MessageResponse("Mot de passe défini avec succès.");
    }

    /** Changement volontaire depuis « Mon Profil » : ancien mot de passe obligatoire. */
    @Transactional
    public MessageResponse changerMotDePasse(ChangementMotDePasseRequest request, Agent agent) {
        if (!passwordEncoder.matches(request.ancienMotDePasse(), agent.getMotDePasse())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ancien mot de passe incorrect.");
        }
        verifierConcordance(request.nouveauMotDePasse(), request.confirmationMotDePasse());
        verifierPolitique(request.nouveauMotDePasse());

        if (passwordEncoder.matches(request.nouveauMotDePasse(), agent.getMotDePasse())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le nouveau mot de passe doit être différent de l'ancien.");
        }

        agent.setMotDePasse(passwordEncoder.encode(request.nouveauMotDePasse()));
        agent.setFirstLogin(false);
        agentRepository.save(agent);

        return new MessageResponse("Mot de passe modifié avec succès.");
    }

    private static void verifierConcordance(String nouveau, String confirmation) {
        if (!nouveau.equals(confirmation)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Les mots de passe ne correspondent pas.");
        }
    }

    /** Politique : 8 caractères minimum, au moins une lettre et un chiffre. */
    private static void verifierPolitique(String motDePasse) {
        if (motDePasse.length() < 8
                || !motDePasse.matches(".*[a-zA-Z].*")
                || !motDePasse.matches(".*\\d.*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le mot de passe doit contenir au moins 8 caractères, "
                    + "dont une lettre et un chiffre.");
        }
    }

    private ResponseStatusException identifiantsInvalides() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Email ou mot de passe incorrect.");
    }
}