package com.afb.transferplatform.controller;

import com.afb.transferplatform.dto.StatistiquesDtos.StatistiquesResponse;
import com.afb.transferplatform.entity.Agent;
import com.afb.transferplatform.service.StatistiquesService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Statistiques. Un seul endpoint : le service adapte automatiquement
 * la portée des données au rôle de l'utilisateur connecté.
 */
@RestController
@RequestMapping("/api/statistiques")
public class StatistiquesController {

    private final StatistiquesService service;

    public StatistiquesController(StatistiquesService service) {
        this.service = service;
    }

    @GetMapping
    public StatistiquesResponse statistiques(@AuthenticationPrincipal Agent utilisateur) {
        return service.pour(utilisateur);
    }
}