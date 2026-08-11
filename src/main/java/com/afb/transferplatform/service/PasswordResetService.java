package com.afb.transferplatform.service;

import com.afb.transferplatform.dto.AuthDtos.*;
import com.afb.transferplatform.entity.Agent;
import com.afb.transferplatform.entity.PasswordResetOtp;
import com.afb.transferplatform.repository.AgentRepository;
import com.afb.transferplatform.repository.PasswordResetOtpRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Flux "Mot de passe oublié" en 3 étapes :
 *  1. forgotPassword  : l'utilisateur saisit son téléphone → OTP 6 chiffres envoyé par SMS (valide 10 min)
 *  2. verifyOtp       : l'utilisateur saisit le code reçu → le code est marqué vérifié
 *  3. resetPassword   : l'utilisateur saisit nouveau mot de passe + confirmation → mise à jour
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private static final Duration VALIDITE_OTP = Duration.ofMinutes(10);
    private static final int MAX_TENTATIVES = 5;

    private final AgentRepository agentRepository;
    private final PasswordResetOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(AgentRepository agentRepository,
                                PasswordResetOtpRepository otpRepository,
                                PasswordEncoder passwordEncoder,
                                EmailService emailService) {
        this.agentRepository = agentRepository;
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /** Étape 1 : génère et envoie l'OTP par SMS. */
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        String email = normaliser(request.email());
        Optional<Agent> agentOpt = agentRepository.findByEmail(email);

        // Réponse générique : on ne révèle pas si un compte existe ou non (anti-énumération)
        if (agentOpt.isEmpty()) {
            log.warn("Demande de réinitialisation pour un email inconnu : {}", email);
            return new MessageResponse(
                    "Si un compte existe avec cet email, un code de vérification a été envoyé.");
        }

        // Invalider les anciens codes puis en émettre un nouveau
        otpRepository.invaliderTous(email);

        String code = genererCode();
        PasswordResetOtp otp = new PasswordResetOtp(
                email,
                passwordEncoder.encode(code),
                Instant.now().plus(VALIDITE_OTP));
        otpRepository.save(otp);

        emailService.envoyer(email, "Réinitialisation de votre mot de passe",
                "Votre code de réinitialisation est : " + code
                        + " (valide 10 minutes). Ne le partagez avec personne.");

        return new MessageResponse(
                "Si un compte existe avec cet email, un code de vérification a été envoyé.");
    }

    /** Étape 2 : vérifie le code OTP saisi. */
    @Transactional
    public MessageResponse verifyOtp(VerifyOtpRequest request) {
        String email = normaliser(request.email());
        PasswordResetOtp otp = chargerOtpValide(email);

        if (!passwordEncoder.matches(request.code(), otp.getCodeHash())) {
            otp.setTentatives(otp.getTentatives() + 1);
            if (otp.getTentatives() >= MAX_TENTATIVES) {
                otp.setUtilise(true); // trop d'essais → code invalidé
                otpRepository.save(otp);
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Trop de tentatives. Veuillez demander un nouveau code.");
            }
            otpRepository.save(otp);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code incorrect.");
        }

        otp.setVerifie(true);
        otpRepository.save(otp);
        return new MessageResponse("Code vérifié. Vous pouvez définir un nouveau mot de passe.");
    }

    /** Étape 3 : réinitialise le mot de passe (le code doit avoir été vérifié). */
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        if (!request.nouveauMotDePasse().equals(request.confirmationMotDePasse())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Les mots de passe ne correspondent pas.");
        }
        if (request.nouveauMotDePasse().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le mot de passe doit contenir au moins 6 caractères.");
        }

        String email = normaliser(request.email());
        PasswordResetOtp otp = chargerOtpValide(email);

        // Sécurité : re-vérifier le code fourni ET exiger qu'il ait été validé à l'étape 2
        if (!otp.isVerifie() || !passwordEncoder.matches(request.code(), otp.getCodeHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Code invalide ou non vérifié.");
        }

        Agent agent = agentRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Compte introuvable."));

        agent.setMotDePasse(passwordEncoder.encode(request.nouveauMotDePasse()));
        agentRepository.save(agent);

        otp.setUtilise(true); // OTP consommé : usage unique
        otpRepository.save(otp);

        log.info("Mot de passe réinitialisé pour l'agent id={}", agent.getId());
        return new MessageResponse("Mot de passe réinitialisé avec succès. Vous pouvez vous connecter.");
    }

    private PasswordResetOtp chargerOtpValide(String email) {
        PasswordResetOtp otp = otpRepository
                .findTopByEmailAndUtiliseFalseOrderByIdDesc(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Aucun code actif. Veuillez demander un nouveau code."));
        if (otp.estExpire()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Code expiré. Veuillez demander un nouveau code.");
        }
        return otp;
    }

    private String genererCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    private String normaliser(String email) {
        return email.trim().toLowerCase();
    }
}
