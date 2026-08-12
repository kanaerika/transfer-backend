package com.afb.transferplatform.controller;
 
 
import com.afb.transferplatform.dto.TransfertDtos.*;
import com.afb.transferplatform.entity.Agent;
import com.afb.transferplatform.service.TransfertService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
 
import java.util.List;
 
@RestController
@RequestMapping("/api/transferts")
public class TransfertController {
 
    private final TransfertService transfertService;
 
    public TransfertController(TransfertService transfertService) {
        this.transfertService = transfertService;
    }
     
 
    /** Vérification du plafond mensuel Hors CEMAC avant exécution. */
    @PostMapping("/verification")
    public VerificationResponse verifier(@Valid @RequestBody VerificationRequest request,
                                         @AuthenticationPrincipal Agent agent) {
        return transfertService.verifier(request, agent);
    }
 
    /** Exécution du transfert (référence plateforme obligatoire). */
    @PostMapping
    public TransfertResponse executer(@Valid @RequestBody ExecutionRequest request,
                                      @AuthenticationPrincipal Agent agent) {
        return transfertService.executer(request, agent);
    }
 
    /** Historique du partenaire de l'agent connecté, filtrable par nom ou référence (?q=...). */
    @GetMapping
    public List<TransfertResponse> historique(@RequestParam(required = false) String q,
                                              @AuthenticationPrincipal Agent agent) {
        return transfertService.historique(q, agent);
    }

    /** Transferts exécutés du partenaire de l'agent connecté, pouvant être annulés. */
    @GetMapping("/annulables")
    public List<TransfertResponse> annulables(@RequestParam(required = false) String q,
                                              @AuthenticationPrincipal Agent agent) {
        return transfertService.annulables(q, agent);
    }
 
    /** Annulation d'un transfert exécuté. */
    @PatchMapping("/{id}/annulation")
    public TransfertResponse annuler(@PathVariable Long id,
                                     @Valid @RequestBody MotifRequest motif,
                                     @AuthenticationPrincipal Agent agent) {
        return transfertService.annuler(id, motif.motif(), agent);
    }
 
    /** Rejet d'un transfert exécuté (motif obligatoire, min. 10 caractères). */
    @PatchMapping("/{id}/rejet")
    public TransfertResponse rejeter(@PathVariable Long id,
                                     @Valid @RequestBody MotifRequest motif,
                                     @AuthenticationPrincipal Agent agent) {
        return transfertService.rejeter(id, motif.motif(), agent);
    }
 
    /** Bilan journalier de l'agent connecté. */
    @GetMapping("/bilan")
    public BilanResponse bilan(@AuthenticationPrincipal Agent agent) {
        return transfertService.bilan(agent);
    }
 
    /** Auto-complétion des clients connus pour faciliter la saisie de l'agent. */
    @GetMapping("/clients")
    public List<ClientConnu> clientsConnus(@RequestParam(defaultValue = "") String q) {
        return transfertService.clientsConnus(q);
    }

    /** Détail d'un transfert pour la page « voir informations ». */
    @GetMapping("/{id}")
    public TransfertResponse detail(@PathVariable Long id,
                                    @AuthenticationPrincipal Agent agent) {
        return transfertService.detail(id, agent);
    }
}
