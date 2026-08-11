package com.afb.transferplatform.controller;

import com.afb.transferplatform.dto.ProfilDtos.*;
import com.afb.transferplatform.entity.Agent;
import com.afb.transferplatform.service.ProfilService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Profil de l'utilisateur connecté (n'importe quel rôle). */
@RestController
@RequestMapping("/api/profil")
public class ProfilController {

    private final ProfilService service;

    public ProfilController(ProfilService service) {
        this.service = service;
    }

    @GetMapping
    public ProfilResponse consulter(@AuthenticationPrincipal Agent agent) {
        return service.consulter(agent);
    }

    @PutMapping
    public ProfilResponse modifier(@Valid @RequestBody ModifierProfilRequest req,
                                   @AuthenticationPrincipal Agent agent) {
        return service.modifier(req, agent);
    }
}
