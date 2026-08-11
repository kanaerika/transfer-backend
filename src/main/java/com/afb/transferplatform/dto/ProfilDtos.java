package com.afb.transferplatform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class ProfilDtos {

    public record ProfilResponse(
            String nomComplet, String email, String agence,
            String role, String partenaireNom) {}

    public record ModifierProfilRequest(
            @NotBlank String nomComplet,
            @NotBlank @Email String email,
            String agence) {}
}
