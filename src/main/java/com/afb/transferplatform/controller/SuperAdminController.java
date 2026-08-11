package com.afb.transferplatform.controller;

import com.afb.transferplatform.dto.AuthDtos.*;
import com.afb.transferplatform.entity.Agent;
import com.afb.transferplatform.service.SuperAdminService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints du Super Administrateur (Afriland First Bank). */
@RestController
@RequestMapping("/api/superadmin/partenaires")
public class SuperAdminController {

    private final SuperAdminService service;

    public SuperAdminController(SuperAdminService service) { this.service = service; }

    @GetMapping
    public List<PartenaireResponse> lister(@AuthenticationPrincipal Agent admin) {
        return service.lister(admin);
    }

    @GetMapping("/{id}")
    public PartenaireResponse detail(@PathVariable Long id, @AuthenticationPrincipal Agent admin) {
        return service.detail(id, admin);
    }

    @DeleteMapping("/{id}")
    public void supprimer(@PathVariable Long id, @AuthenticationPrincipal Agent admin) {
        service.supprimer(id, admin);
    }

    @PostMapping
    public PartenaireResponse creer(@Valid @RequestBody PartenaireRequest req,
                                    @AuthenticationPrincipal Agent admin) {
        return service.creer(req, admin);
    }

    @PutMapping("/{id}")
    public PartenaireResponse modifier(@PathVariable Long id,
                                       @Valid @RequestBody ModificationPartenaireRequest req,
                                       @AuthenticationPrincipal Agent admin) {
        return service.modifier(id, req, admin);
    }

    @PatchMapping("/{id}/activation")
    public PartenaireResponse basculerActif(@PathVariable Long id,
                                            @AuthenticationPrincipal Agent admin) {
        return service.basculerActif(id, admin);
    }

    @PostMapping("/{id}/invitation")
    public MessageResponse reinviter(@PathVariable Long id,
                                     @AuthenticationPrincipal Agent admin) {
        return service.reinviter(id, admin);
    }
}
