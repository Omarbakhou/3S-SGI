package com.SSS.SGI.controller;

import com.SSS.SGI.entity.Affectation;
import com.SSS.SGI.service.AffectationService;
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
public class AffectationController {

    private final AffectationService affectationService;

    public AffectationController(AffectationService affectationService) {
        this.affectationService = affectationService;
    }

    /**
     * Crée une affectation entre un collaborateur et un projet
     * Accessible uniquement par les managers/administrateurs
     */
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

    /**
     * Récupère une affectation spécifique
     * Accessible par les managers et employés
     */
    @GetMapping("/{collaborateurId}/{projetId}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER')")
    public ResponseEntity<Affectation> getAffectation(
            @PathVariable Long collaborateurId,
            @PathVariable Long projetId) {
        Optional<Affectation> affectation = affectationService.getAffectation(collaborateurId, projetId);
        return affectation.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Récupère toutes les affectations d'un collaborateur
     * Accessible par le collaborateur lui-même, les managers et administrateurs
     */
    @GetMapping("/collaborateur/{collaborateurId}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<Affectation>> getAffectationsByCollaborateur(@PathVariable Long collaborateurId) {
        List<Affectation> affectations = affectationService.getAffectationsByCollaborateur(collaborateurId);
        return ResponseEntity.ok(affectations);
    }

    /**
     * Récupère toutes les affectations d'un projet
     * Accessible par les managers et administrateurs
     */
    @GetMapping("/projet/{projetId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<Affectation>> getAffectationsByProjet(@PathVariable Long projetId) {
        List<Affectation> affectations = affectationService.getAffectationsByProjet(projetId);
        return ResponseEntity.ok(affectations);
    }

    /**
     * Récupère toutes les affectations
     * Accessible uniquement par les administrateurs
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Affectation>> getAllAffectations() {
        List<Affectation> affectations = affectationService.getAllAffectations();
        return ResponseEntity.ok(affectations);
    }

    /**
     * Met à jour le taux d'affectation
     * Accessible uniquement par les managers/administrateurs
     */
    @PutMapping("/{collaborateurId}/{projetId}/taux")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Affectation> updateTauxAffectation(
            @PathVariable Long collaborateurId,
            @PathVariable Long projetId,
            @RequestParam BigDecimal nouveauTaux) {
        Affectation updated = affectationService.updateTauxAffectation(collaborateurId, projetId, nouveauTaux);
        return ResponseEntity.ok(updated);
    }

    /**
     * Supprime une affectation
     * Accessible uniquement par les managers/administrateurs
     */
    @DeleteMapping("/{collaborateurId}/{projetId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<String> deleteAffectation(
            @PathVariable Long collaborateurId,
            @PathVariable Long projetId) {
        affectationService.deleteAffectation(collaborateurId, projetId);
        return ResponseEntity.ok("Affectation supprimée avec succès");
    }

    /**
     * Calcule le taux d'affectation total d'un collaborateur
     * Accessible par le collaborateur lui-même, les managers et administrateurs
     */
    @GetMapping("/collaborateur/{collaborateurId}/taux-total")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<BigDecimal> getTauxAffectationTotal(@PathVariable Long collaborateurId) {
        BigDecimal tauxTotal = affectationService.getTauxAffectationTotal(collaborateurId);
        return ResponseEntity.ok(tauxTotal);
    }

    /**
     * Vérifie si un collaborateur peut être affecté à un nouveau projet
     * Accessible par les managers et administrateurs
     */
    @GetMapping("/collaborateur/{collaborateurId}/can-affect")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Boolean> canAffectCollaborateur(
            @PathVariable Long collaborateurId,
            @RequestParam BigDecimal nouveauTaux) {
        boolean canAffect = affectationService.canAffectCollaborateur(collaborateurId, nouveauTaux);
        return ResponseEntity.ok(canAffect);
    }
}


