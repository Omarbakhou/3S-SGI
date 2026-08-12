package com.SSS.SGI.controller;

import com.SSS.SGI.entity.Imputation;
import com.SSS.SGI.entity.StatutImputation;
import com.SSS.SGI.service.ImputationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Contrôleur pour gérer les imputations
 * Les employés peuvent faire CRUD sur les imputations
 * Les managers peuvent valider/rejeter les imputations
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

    @Operation(summary = "Créer une imputation", description = "Accessible uniquement par les employés.")
    @ApiResponse(responseCode = "201", description = "Imputation créée")
    @PostMapping
    @PreAuthorize("hasRole('EMPLOYE')")
    public ResponseEntity<Imputation> createImputation(@RequestBody Imputation imputation) {
        Imputation created = imputationService.createImputation(imputation);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @Operation(summary = "Récupérer une imputation par ID", description = "Accessible par les employés et managers.")
    @ApiResponse(responseCode = "200", description = "Imputation trouvée")
    @ApiResponse(responseCode = "404", description = "Imputation introuvable")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER')")
    public ResponseEntity<Imputation> getImputation(@PathVariable Long id) {
        Optional<Imputation> imputation = imputationService.getImputationById(id);
        return imputation.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Lister toutes les imputations", description = "Accessible uniquement par les managers.")
    @ApiResponse(responseCode = "200", description = "Liste de toutes les imputations")
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<List<Imputation>> getAllImputations() {
        List<Imputation> imputations = imputationService.getAllImputations();
        return ResponseEntity.ok(imputations);
    }

    @Operation(summary = "Lister les imputations d'un employé", description = "Accessible par l'employé lui-même et les managers.")
    @ApiResponse(responseCode = "200", description = "Liste des imputations de l'employé")
    @GetMapping("/employe/{employeId}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER')")
    public ResponseEntity<List<Imputation>> getImputationsByEmploye(@PathVariable Long employeId) {
        List<Imputation> imputations = imputationService.getImputationsByEmploye(employeId);
        return ResponseEntity.ok(imputations);
    }

    @Operation(summary = "Lister les imputations d'un projet", description = "Accessible par les managers.")
    @ApiResponse(responseCode = "200", description = "Liste des imputations du projet")
    @GetMapping("/projet/{projetId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<Imputation>> getImputationsByProjet(@PathVariable Long projetId) {
        List<Imputation> imputations = imputationService.getImputationsByProjet(projetId);
        return ResponseEntity.ok(imputations);
    }

    @Operation(summary = "Lister les imputations d'un employé sur un projet", description = "Accessible par les managers.")
    @ApiResponse(responseCode = "200", description = "Liste des imputations de l'employé sur le projet")
    @GetMapping("/employe/{employeId}/projet/{projetId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<Imputation>> getImputationsByEmployeAndProjet(
            @PathVariable Long employeId,
            @PathVariable Long projetId) {
        List<Imputation> imputations = imputationService.getImputationsByEmployeIdAndProjetId(employeId, projetId);
        return ResponseEntity.ok(imputations);
    }

    @Operation(summary = "Lister les imputations d'un employé selon leur statut", description = "Accessible par les managers.")
    @ApiResponse(responseCode = "200", description = "Liste des imputations filtrées par statut")
    @GetMapping("/employe/{employeId}/statut/{statut}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<Imputation>> getImputationsByEmployeAndStatut(@PathVariable Long employeId, @PathVariable StatutImputation statut) {
        List<Imputation> imputations = imputationService.getImputationsByEmployeAndStatut(employeId, statut);
        return ResponseEntity.ok(imputations);
    }

    @Operation(summary = "Lister les imputations rattachées à un manager", description = "Accessible par les managers.")
    @ApiResponse(responseCode = "200", description = "Liste des imputations du manager")
    @GetMapping("/manager/{managerId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<Imputation>> getImputationsByManager(@PathVariable Long managerId) {
        List<Imputation> imputations = imputationService.getImputationsByManager(managerId);
        return ResponseEntity.ok(imputations);
    }

    @Operation(summary = "Lister les imputations en attente de validation pour un manager", description = "Accessible par les managers.")
    @ApiResponse(responseCode = "200", description = "Liste des imputations EN_ATTENTE pour ce manager")
    @GetMapping("/en-attente")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<Imputation>> getImputationsEnAttente(@RequestParam Long managerId) {
        List<Imputation> imputations = imputationService.getImputationsEnAttenteForManager(managerId);
        return ResponseEntity.ok(imputations);
    }

    @Operation(summary = "Mettre à jour une imputation", description = "Accessible uniquement par l'employé propriétaire (imputations en attente uniquement).")
    @ApiResponse(responseCode = "200", description = "Imputation mise à jour")
    @ApiResponse(responseCode = "404", description = "Imputation introuvable")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ResponseEntity<Imputation> updateImputation(
            @PathVariable Long id,
            @RequestParam Long employeId,
            @RequestBody Imputation updatedImputation) {
        Imputation updated = imputationService.updateImputation(id, employeId, updatedImputation);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Supprimer une imputation", description = "Accessible uniquement par l'employé propriétaire (imputations en attente uniquement).")
    @ApiResponse(responseCode = "200", description = "Imputation supprimée")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ResponseEntity<String> deleteImputation(
            @PathVariable Long id,
            @RequestParam Long employeId) {
        imputationService.deleteImputation(id, employeId);
        return ResponseEntity.ok("Imputation supprimée avec succès");
    }

    @Operation(summary = "Valider une imputation", description = "Accessible uniquement par les managers.")
    @ApiResponse(responseCode = "200", description = "Imputation validée")
    @PostMapping("/{imputationId}/valider")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Imputation> validerImputation(
            @PathVariable Long imputationId,
            @RequestParam Long managerId) {
        Imputation validated = imputationService.validerImputation(imputationId, managerId);
        return ResponseEntity.ok(validated);
    }

    @Operation(summary = "Rejeter une imputation", description = "Accessible uniquement par les managers.")
    @ApiResponse(responseCode = "200", description = "Imputation rejetée")
    @PostMapping("/{imputationId}/rejeter")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Imputation> rejeterImputation(
            @PathVariable Long imputationId,
            @RequestParam Long managerId) {
        Imputation rejected = imputationService.rejeterImputation(imputationId, managerId);
        return ResponseEntity.ok(rejected);
    }
}