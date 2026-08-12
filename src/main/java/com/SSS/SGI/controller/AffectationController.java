package com.SSS.SGI.controller;

import com.SSS.SGI.entity.Affectation;
import com.SSS.SGI.service.AffectationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Contrôleur pour gérer les affectations
 * Gère la relation many-to-many entre Collaborateur et Projet
 * avec le taux d'affectation (décision métier)
 */
@RestController
@RequestMapping("/api/affectations")
@CrossOrigin(origins = "*")
@Tag(name = "Affectations", description = "Affectation des collaborateurs aux projets et taux d'affectation")
public class AffectationController {

    private final AffectationService affectationService;

    public AffectationController(AffectationService affectationService) {
        this.affectationService = affectationService;
    }

    @Operation(summary = "Créer une affectation", description = "Affecte un collaborateur à un projet avec un taux. Accessible par managers/administrateurs.")
    @ApiResponse(responseCode = "201", description = "Affectation créée")
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Affectation> createAffectation(@RequestBody CreateAffectationRequest request) {
        Affectation created = affectationService.createAffectation(
            request.getCollaborateurId(),
            request.getProjetId(),
            request.getTauxAffectation(),
            request.getDateAffectation()
        );
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @Operation(summary = "Récupérer une affectation", description = "Accessible par les managers et employés.")
    @ApiResponse(responseCode = "200", description = "Affectation trouvée")
    @ApiResponse(responseCode = "404", description = "Affectation introuvable")
    @GetMapping("/{collaborateurId}/{projetId}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER')")
    public ResponseEntity<Affectation> getAffectation(
            @PathVariable Long collaborateurId,
            @PathVariable Long projetId) {
        Optional<Affectation> affectation = affectationService.getAffectation(collaborateurId, projetId);
        return affectation.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Lister les affectations d'un collaborateur", description = "Accessible par le collaborateur lui-même, les managers et administrateurs.")
    @ApiResponse(responseCode = "200", description = "Liste des affectations du collaborateur")
    @GetMapping("/collaborateur/{collaborateurId}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<Affectation>> getAffectationsByCollaborateur(@PathVariable Long collaborateurId) {
        List<Affectation> affectations = affectationService.getAffectationsByCollaborateur(collaborateurId);
        return ResponseEntity.ok(affectations);
    }

    @Operation(summary = "Lister les affectations d'un projet", description = "Accessible par les managers et administrateurs.")
    @ApiResponse(responseCode = "200", description = "Liste des affectations du projet")
    @GetMapping("/projet/{projetId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<Affectation>> getAffectationsByProjet(@PathVariable Long projetId) {
        List<Affectation> affectations = affectationService.getAffectationsByProjet(projetId);
        return ResponseEntity.ok(affectations);
    }

    @Operation(summary = "Lister toutes les affectations", description = "Accessible uniquement par les administrateurs.")
    @ApiResponse(responseCode = "200", description = "Liste de toutes les affectations")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Affectation>> getAllAffectations() {
        List<Affectation> affectations = affectationService.getAllAffectations();
        return ResponseEntity.ok(affectations);
    }

    @Operation(summary = "Mettre à jour le taux d'affectation", description = "Accessible uniquement par les managers/administrateurs.")
    @ApiResponse(responseCode = "200", description = "Taux d'affectation mis à jour")
    @ApiResponse(responseCode = "404", description = "Affectation introuvable")
    @PutMapping("/{collaborateurId}/{projetId}/taux")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Affectation> updateTauxAffectation(
            @PathVariable Long collaborateurId,
            @PathVariable Long projetId,
            @RequestParam BigDecimal nouveauTaux) {
        Affectation updated = affectationService.updateTauxAffectation(collaborateurId, projetId, nouveauTaux);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Supprimer une affectation", description = "Accessible uniquement par les managers/administrateurs.")
    @ApiResponse(responseCode = "200", description = "Affectation supprimée")
    @DeleteMapping("/{collaborateurId}/{projetId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<String> deleteAffectation(
            @PathVariable Long collaborateurId,
            @PathVariable Long projetId) {
        affectationService.deleteAffectation(collaborateurId, projetId);
        return ResponseEntity.ok("Affectation supprimée avec succès");
    }

    @Operation(summary = "Calculer le taux d'affectation total d'un collaborateur", description = "Accessible par le collaborateur lui-même, les managers et administrateurs.")
    @ApiResponse(responseCode = "200", description = "Taux d'affectation total (somme sur tous les projets)")
    @GetMapping("/collaborateur/{collaborateurId}/taux-total")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<BigDecimal> getTauxAffectationTotal(@PathVariable Long collaborateurId) {
        BigDecimal tauxTotal = affectationService.getTauxAffectationTotal(collaborateurId);
        return ResponseEntity.ok(tauxTotal);
    }

    @Operation(summary = "Vérifier si un collaborateur peut être affecté à un nouveau taux", description = "Accessible par les managers et administrateurs.")
    @ApiResponse(responseCode = "200", description = "true si le taux total resterait dans la limite autorisée")
    @GetMapping("/collaborateur/{collaborateurId}/can-affect")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Boolean> canAffectCollaborateur(
            @PathVariable Long collaborateurId,
            @RequestParam BigDecimal nouveauTaux) {
        boolean canAffect = affectationService.canAffectCollaborateur(collaborateurId, nouveauTaux);
        return ResponseEntity.ok(canAffect);
    }
}