package com.afb.transferplatform.dto;
 
import com.afb.transferplatform.entity.Transfert;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
 
import java.time.LocalDate;
import java.util.List;
 
public class TransfertDtos {
 
    public record VerificationRequest(
            @NotBlank String nomClient,
            @NotBlank String dateNaissance,
            @NotBlank String naturePiece,
            @NotBlank String numeroPiece,
            @Min(1) long montant,
            @NotBlank String paysDestination) {}
 
    public record VerificationResponse(
            boolean autorise,
            String message,
            long plafond,
            long cumulMois,
            long montantDemande,
            long montantRestant,
            int pourcentageUtilise,
            int pourcentageApres,
            DernierTransfert dernierTransfert,
            Long transfertId) {}
 
    public record DernierTransfert(
            String nomClient,
            String dateTransfert,
            long montant,
            String statut) {}
 
    public record ExecutionRequest(
            @NotBlank String nomClient,
            @NotBlank String dateNaissance,
            @NotBlank String naturePiece,
            @NotBlank String numeroPiece,
            @Min(1) long montant,
            @NotBlank String paysDestination,
            @NotBlank String reference,
            String canal,
            Long transfertId) {}

    /** Clôture différée d'un transfert resté NON_CLOTURE (référence saisie plus tard). */
    public record ClotureRequest(
            @NotBlank String reference,
            String canal) {}
 
    /** Motif d'annulation ou de rejet (min. 10 caractères). */
    public record MotifRequest(
            @NotBlank @Size(min = 10, message = "Le motif doit contenir au moins 10 caractères.")
            String motif) {}
 
    public record TransfertResponse(
            Long id,
            String nomClient,
            String dateNaissance,
            String naturePiece,
            String numeroPiece,
            long montant,
            String paysDestination,
            String statut,
            String reference,
            String referenceVerification,
            String agence,
            String canal,
            LocalDate dateTransfert,
            long cumulMois,
            String motif) {

        public static TransfertResponse from(Transfert t) {
            return new TransfertResponse(
                    t.getId(), t.getNomClient(), t.getDateNaissance(), t.getNaturePiece(),
                    t.getNumeroPiece(), t.getMontant(), t.getPaysDestination(),
                    t.getStatut().getLibelle(), t.getReference(), t.getReferenceVerification(), t.getAgence(),
                    t.getCanal(), t.getDateTransfert(), t.getCumulMois(), t.getMotif());
        }
    }
 
    public record BilanResponse(
            LocalDate jour,
            int executes,
            int rejetes,
            int annules,
            int nonClotures,
            int total,
            List<TransfertResponse> lignes) {}
 
    public record ReferentielResponse(
            List<String> naturesPiece,
            List<String> pays,
            List<String> roles,
            List<CanalDto> canaux,
            long plafond) {}
 
    public record CanalDto(String nom, String description) {}
 
    /** Client déjà présent en base, proposé en auto-complétion à l'agent. */
    public record ClientConnu(
            String nomClient,
            String dateNaissance,
            String naturePiece,
            String numeroPiece) {}

    /** Contrôle en lecture seule (sans enregistrement) du plafond déjà atteint par un client. */
    public record PlafondClientResponse(
            long cumul,
            long plafond,
            boolean depasse) {}
}
 
