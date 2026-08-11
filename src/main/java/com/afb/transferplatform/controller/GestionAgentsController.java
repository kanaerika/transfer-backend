package com.afb.transferplatform.controller;

import com.afb.transferplatform.dto.AuthDtos.*;
import com.afb.transferplatform.entity.Agent;
import com.afb.transferplatform.service.GestionAgentsService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints de l'Administrateur du partenaire : gestion de SES agents. */
@RestController
@RequestMapping("/api/partenaire/agents")
public class GestionAgentsController {

    private final GestionAgentsService service;

    public GestionAgentsController(GestionAgentsService service) { this.service = service; }

    @GetMapping
    public List<UtilisateurResponse> lister(@AuthenticationPrincipal Agent admin) {
        return service.lister(admin);
    }

    @PostMapping
    public UtilisateurResponse creer(@Valid @RequestBody AgentRequest req,
                                     @AuthenticationPrincipal Agent admin) {
        return service.creer(req, admin);
    }

    @PutMapping("/{id}")
    public UtilisateurResponse modifier(@PathVariable Long id,
                                        @Valid @RequestBody AgentRequest req,
                                        @AuthenticationPrincipal Agent admin) {
        return service.modifier(id, req, admin);
    }

    @PatchMapping("/{id}/activation")
    public UtilisateurResponse basculerActif(@PathVariable Long id,
                                             @AuthenticationPrincipal Agent admin) {
        return service.basculerActif(id, admin);
    }

    @PostMapping("/{id}/invitation")
    public MessageResponse reinviter(@PathVariable Long id,
                                     @AuthenticationPrincipal Agent admin) {
        return service.reinviter(id, admin);
    }

    @PostMapping("/{id}/reinitialisation")
    public MessageResponse reinitialiser(@PathVariable Long id,
                                         @AuthenticationPrincipal Agent admin) {
        return service.reinitialiserMotDePasse(id, admin);
    }
    @DeleteMapping("/{id}")
public MessageResponse supprimer(@PathVariable Long id,
                                 @AuthenticationPrincipal Agent admin) {
    return service.supprimer(id, admin);
}
}
