package com.afb.transferplatform.repository;

import com.afb.transferplatform.entity.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {

    /** Dernier OTP actif (non utilisé) pour ce numéro */
    Optional<PasswordResetOtp> findTopByEmailAndUtiliseFalseOrderByIdDesc(String email);

    /** Invalide tous les anciens OTP d'un numéro avant d'en émettre un nouveau */
    @Modifying
    @Query("UPDATE PasswordResetOtp o SET o.utilise = true WHERE o.email = :email AND o.utilise = false")
    void invaliderTous(@Param("email") String email);
}
