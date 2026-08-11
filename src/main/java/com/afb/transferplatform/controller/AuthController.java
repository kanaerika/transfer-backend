package com.afb.transferplatform.controller;

import com.afb.transferplatform.dto.AuthDtos.*;
import com.afb.transferplatform.service.AuthService;
import com.afb.transferplatform.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import com.afb.transferplatform.entity.Agent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService,
                          PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Activation du compte via le lien d'invitation reçu par email. */
    /** Première connexion : mot de passe obligatoire, sans ancien mot de passe. */
    @PostMapping("/premier-mot-de-passe")
    public MessageResponse definirPremierMotDePasse(
            @Valid @RequestBody PremierMotDePasseRequest request,
            @AuthenticationPrincipal Agent agent) {
        return authService.definirPremierMotDePasse(request, agent);
    }

    /** Changement depuis « Mon Profil » : ancien mot de passe requis. */
    @PostMapping("/changer-mot-de-passe")
    public MessageResponse changerMotDePasse(
            @Valid @RequestBody ChangementMotDePasseRequest request,
            @AuthenticationPrincipal Agent agent) {
        return authService.changerMotDePasse(request, agent);
    }

    // ---- Mot de passe oublié : OTP par email ----

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return passwordResetService.forgotPassword(request);
    }

    @PostMapping("/verify-otp")
    public MessageResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return passwordResetService.verifyOtp(request);
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return passwordResetService.resetPassword(request);
    }
}
