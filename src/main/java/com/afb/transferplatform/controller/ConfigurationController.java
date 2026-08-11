package com.afb.transferplatform.controller;

import com.afb.transferplatform.dto.ConfigurationDtos.*;
import com.afb.transferplatform.entity.Agent;
import com.afb.transferplatform.entity.Configuration;
import com.afb.transferplatform.service.ConfigurationService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Configuration globale de la plateforme. Réservé aux administrateurs. */
@RestController
@RequestMapping("/api/configuration")
public class ConfigurationController {

    private final ConfigurationService service;

    public ConfigurationController(ConfigurationService service) {
        this.service = service;
    }

    @GetMapping
    public ConfigurationResponse lire() {
        return versReponse(service.lire());
    }

    @PutMapping("/plafond")
    public ConfigurationResponse modifierPlafond(
            @Valid @RequestBody ModifierPlafondRequest req,
            @AuthenticationPrincipal Agent admin) {
        return versReponse(service.modifierPlafond(req.plafondMensuel(), admin));
    }

    private ConfigurationResponse versReponse(Configuration c) {
        return new ConfigurationResponse(
                c.getPlafondMensuel(),
                c.getModifiePar(),
                c.getModifieLe() != null ? c.getModifieLe().toString() : null);
    }
}