package com.afb.transferplatform.controller;

import com.afb.transferplatform.dto.AuthDtos.MessageResponse;
import com.afb.transferplatform.service.InvitationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invitation")
@CrossOrigin(origins = "http://localhost:4200")
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    /**
     * Valide l'invitation. Ne renvoie aucune donnée sensible :
     * l'endpoint est public, il ne doit pas exposer l'entité Agent.
     */
    @PostMapping("/validation")
    public MessageResponse valider(@RequestParam String token) {
        invitationService.validerInvitation(token);
        return new MessageResponse(
                "Invitation validée. Connectez-vous avec le mot de passe temporaire reçu par email.");
    }
}