package com.afb.transferplatform.dto;

import jakarta.validation.constraints.Positive;

public class ConfigurationDtos {

    public record ConfigurationResponse(
            long plafondMensuel, String modifiePar, String modifieLe) {}

    public record ModifierPlafondRequest(
            @Positive(message = "Le plafond doit être un montant positif.")
            long plafondMensuel) {}
}