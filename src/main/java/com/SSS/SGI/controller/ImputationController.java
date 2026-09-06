package com.SSS.SGI.controller;

import com.SSS.SGI.dto.CreateImputationRequest;
import com.SSS.SGI.dto.ImputationDTO;
import com.SSS.SGI.dto.RejeterImputationRequest;
import com.SSS.SGI.entity.StatutImputation;
import com.SSS.SGI.security.CustomUserDetails;
import com.SSS.SGI.security.IdentiteAppelant;
import com.SSS.SGI.service.ImputationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur pour gérer les imputations
 * Les employés peuvent faire CRUD sur les imputations
 * Les managers peuvent valider/rejeter les imputations
 *
 * Les identifiants d'appelant présents dans les URL (employeId, managerId) sont conservés pour
 * ne pas rompre le contrat public, mais ne font plus autorité : ils sont confrontés au jeton via
 * {@link IdentiteAppelant} et c'est l'id du principal qui atteint le service.
 */
@RestController
@RequestMapping("/api/imputations")
@CrossOrigin(origins = "*")
@Tag(name = "Imputations", description = "Saisie et validation des imputations de temps sur les projets")
public class ImputationController {

    private final ImputationService imputationService;

    public ImputationController(ImputationService imputationService) {
        this.imputationService = imputationService;
    }

    @Operation(summary = "Créer une imputation", description = "Accessible uniquement par les employés. "
            + "L'imputation est créée pour le porteur du jeton : `employeId` doit désigner l'appelant lui-même.")
    @ApiResponse(responseCode = "201", description = "Imputation créée, statut EN_ATTENTE")
    @ApiResponse(responseCode = "400", description = "Heures invalides, employé non affecté au projet ou doublon")
    @ApiResponse(responseCode = "403", description = "`employeId` désigne un autre collaborateur que le porteur du jeton")
    @ApiResponse(responseCode = "404", description = "Employé ou projet introuvable")
    @PostMapping("/employe/{employeId}")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ResponseEntity<ImputationDTO> createImputation(
            @PathVariable Long employeId,
            @Valid @RequestBody CreateImputationRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        ImputationDTO created = imputationService.creerImputation(
                IdentiteAppelant.resoudre(employeId, principal), request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @Operation(summary = "Récupérer une imputation par ID", description = "Accessible par les employés et managers.")
    @ApiResponse(responseCode = "200", description = "Imputation trouvée")
    @ApiResponse(responseCode = "404", description = "Imputation introuvable")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER')")
    public ResponseEntity<ImputationDTO> getImputation(@PathVariable Long id) {
        return ResponseEntity.ok(imputationService.getImputation(id));
    }

    @Operation(summary = "Lister toutes les imputations", description = "Accessible uniquement par les managers.")
    @ApiResponse(responseCode = "200", description = "Liste de toutes les imputations")
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<List<ImputationDTO>> getAllImputations() {
        return ResponseEntity.ok(imputationService.getAllImputations());
    }

    @Operation(summary = "Lister les imputations d'un employé", description = "Accessible par l'employé lui-même et les managers. "
            + "Un employé ne peut consulter que son propre historique.")
    @ApiResponse(responseCode = "200", description = "Liste des imputations de l'employé")
    @ApiResponse(responseCode = "403", description = "Un employé consulte l'historique d'un autre collaborateur")
    @GetMapping("/employe/{employeId}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER')")
    public ResponseEntity<List<ImputationDTO>> getImputationsByEmploye(
            @PathVariable Long employeId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        IdentiteAppelant.exigerProprietaireSiSimpleEmploye(employeId, principal);
        return ResponseEntity.ok(imputationService.getImputationsByEmploye(employeId));
    }

    @Operation(summary = "Lister les imputations d'un projet", description = "Accessible par les managers.")
    @ApiResponse(responseCode = "200", description = "Liste des imputations du projet")
    @GetMapping("/projet/{projetId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<ImputationDTO>> getImputationsByProjet(@PathVariable Long projetId) {
        return ResponseEntity.ok(imputationService.getImputationsByProjet(projetId));
    }

    @Operation(summary = "Cumul des heures validées sur un projet", description = "Accessible par les managers. Finalité du système d'imputations.")
    @ApiResponse(responseCode = "200", description = "Total d'heures validées sur le projet")
    @ApiResponse(responseCode = "404", description = "Projet introuvable")
    @GetMapping("/projet/{projetId}/cumul-heures")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Double> getCumulHeuresProjet(@PathVariable Long projetId) {
        return ResponseEntity.ok(imputationService.getCumulHeuresValideesByProjet(projetId));
    }

    @Operation(summary = "Lister les imputations d'un employé sur un projet", description = "Accessible par les managers.")
    @ApiResponse(responseCode = "200", description = "Liste des imputations de l'employé sur le projet")
    @GetMapping("/employe/{employeId}/projet/{projetId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<ImputationDTO>> getImputationsByEmployeAndProjet(
            @PathVariable Long employeId,
            @PathVariable Long projetId) {
        return ResponseEntity.ok(imputationService.getImputationsByEmployeIdAndProjetId(employeId, projetId));
    }

    @Operation(summary = "Lister les imputations d'un employé selon leur statut", description = "Accessible par les managers.")
    @ApiResponse(responseCode = "200", description = "Liste des imputations filtrées par statut")
    @GetMapping("/employe/{employeId}/statut/{statut}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<ImputationDTO>> getImputationsByEmployeAndStatut(@PathVariable Long employeId, @PathVariable StatutImputation statut) {
        return ResponseEntity.ok(imputationService.getImputationsByEmployeAndStatut(employeId, statut));
    }

    @Operation(summary = "Lister les imputations rattachées à un manager", description = "Accessible par les managers.")
    @ApiResponse(responseCode = "200", description = "Liste des imputations du manager")
    @GetMapping("/manager/{managerId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<ImputationDTO>> getImputationsByManager(
            @PathVariable Long managerId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(imputationService.getImputationsByManager(
                IdentiteAppelant.resoudre(managerId, principal)));
    }

    @Operation(summary = "Lister les imputations en attente de validation pour un manager", description = "Accessible par les managers. Uniquement celles de ses propres employés.")
    @ApiResponse(responseCode = "200", description = "Liste des imputations EN_ATTENTE pour ce manager")
    @GetMapping("/en-attente")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<ImputationDTO>> getImputationsEnAttente(
            @RequestParam(required = false) Long managerId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(imputationService.getImputationsEnAttenteForManager(
                IdentiteAppelant.resoudre(managerId, principal)));
    }

    @Operation(summary = "Mettre à jour une imputation", description = "Accessible uniquement par l'employé propriétaire (imputations en attente uniquement).")
    @ApiResponse(responseCode = "200", description = "Imputation mise à jour")
    @ApiResponse(responseCode = "400", description = "Heures invalides, employé non affecté au projet ou doublon")
    @ApiResponse(responseCode = "404", description = "Imputation ou projet introuvable")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ResponseEntity<ImputationDTO> updateImputation(
            @PathVariable Long id,
            @RequestParam(required = false) Long employeId,
            @Valid @RequestBody CreateImputationRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(imputationService.updateImputation(
                id, IdentiteAppelant.resoudre(employeId, principal), request));
    }

    @Operation(summary = "Supprimer une imputation", description = "Accessible uniquement par l'employé propriétaire (imputations en attente uniquement).")
    @ApiResponse(responseCode = "200", description = "Imputation supprimée")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ResponseEntity<String> deleteImputation(
            @PathVariable Long id,
            @RequestParam(required = false) Long employeId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        imputationService.deleteImputation(id, IdentiteAppelant.resoudre(employeId, principal));
        return ResponseEntity.ok("Imputation supprimée avec succès");
    }

    @Operation(summary = "Valider une imputation", description = "Accessible uniquement par les managers.")
    @ApiResponse(responseCode = "200", description = "Imputation validée")
    @PostMapping("/{imputationId}/valider")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ImputationDTO> validerImputation(
            @PathVariable Long imputationId,
            @RequestParam(required = false) Long managerId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(imputationService.validerImputation(
                imputationId, IdentiteAppelant.resoudre(managerId, principal)));
    }

    @Operation(summary = "Rejeter une imputation", description = "Accessible uniquement par les managers.")
    @ApiResponse(responseCode = "200", description = "Imputation rejetée")
    @PostMapping("/{imputationId}/rejeter")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ImputationDTO> rejeterImputation(
            @PathVariable Long imputationId,
            @RequestParam(required = false) Long managerId,
            @Valid @RequestBody RejeterImputationRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(imputationService.rejeterImputation(
                imputationId, IdentiteAppelant.resoudre(managerId, principal), request.motif()));
    }
}
