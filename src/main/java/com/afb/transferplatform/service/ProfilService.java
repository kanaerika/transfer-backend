package com.afb.transferplatform.service;

import com.afb.transferplatform.dto.ProfilDtos.*;
import com.afb.transferplatform.entity.Agent;
import com.afb.transferplatform.repository.AgentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Consultation et modification du profil de l'utilisateur connecté (nom, agence). */
@Service
public class ProfilService {

    private final AgentRepository agents;

    public ProfilService(AgentRepository agents) {
        this.agents = agents;
    }

    public ProfilResponse consulter(Agent agent) {
        return versReponse(agent);
    }

    @Transactional
    public ProfilResponse modifier(ModifierProfilRequest req, Agent agent) {
        String email = req.email().trim().toLowerCase();
        if (!email.equals(agent.getEmail())) {
            if (agents.existsByEmailIgnoreCase(email)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Cette adresse email est déjà utilisée.");
            }
            agent.setEmail(email);
        }
        agent.setNomComplet(req.nomComplet().trim());
        agent.setAgence(req.agence());
        agents.save(agent);
        return versReponse(agent);
    }

    private ProfilResponse versReponse(Agent a) {
        return new ProfilResponse(a.getNomComplet(), a.getEmail(), a.getAgence(),
                a.getRole(), a.getNomPartenaire());
    }
}
