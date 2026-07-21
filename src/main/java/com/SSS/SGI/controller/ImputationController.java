package com.SSS.SGI.controller;

import com.SSS.SGI.entity.Imputation;
import com.SSS.SGI.entity.StatutImputation;
import com.SSS.SGI.service.ImputationService;
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
public class ImputationController {

    private final ImputationService imputationService;

    public ImputationController(ImputationService imputationService) {
        this.imputationService = imputationService;
    }

    /**
     * Crée une nouvelle imputation
     * Accessible uniquement par les employés
     */
    @PostMapping
    @PreAuthorize("hasRole('EMPLOYE')")
    public ResponseEntity<Imputation> createImputation(@RequestBody Imputation imputation) {
        Imputation created = imputationService.createImputation(imputation);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Récupère une imputation par son ID
     * Accessible par les employés et managers
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER')")
    public ResponseEntity<Imputation> getImputation(@PathVariable Long id) {
        Optional<Imputation> imputation = imputationService.getImputationById(id);
        return imputation.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Récupère toutes les imputations
     * Accessible uniquement par  managers
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<List<Imputation>> getAllImputations() {
        List<Imputation> imputations = imputationService.getAllImputations();
        return ResponseEntity.ok(imputations);
    }

    /**
     * Récupère les imputations d'un employé
     * Accessible par l'employé lui-même et les managers
     */
    @GetMapping("/employe/{employeId}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER')")
    public ResponseEntity<List<Imputation>> getImputationsByEmploye(@PathVariable Long employeId) {
        List<Imputation> imputations = imputationService.getImputationsByEmploye(employeId);
        return ResponseEntity.ok(imputations);
    }

    /**
     * Récupère les imputations d'un projet
     * Accessible par les managers
     */
    @GetMapping("/projet/{projetId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<Imputation>> getImputationsByProjet(@PathVariable Long projetId) {
        List<Imputation> imputations = imputationService.getImputationsByProjet(projetId);
        return ResponseEntity.ok(imputations);
    }

    /**
     * Récupère les imputations d'un employe et dun projet
     * Accessible par les managers
     */
    @GetMapping("/employe/{employeId}/projet/{projetId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<Imputation>> getImputationsByEmployeAndProjet(
            @PathVariable Long employeId,
            @PathVariable Long projetId) {
        List<Imputation> imputations = imputationService.getImputationsByEmployeIdAndProjetId(employeId, projetId);
        return ResponseEntity.ok(imputations);
    }


    /**
     * Récupère les imputations d'un employe avec un statut spécifique
     * Accessible par les managers
     */
    @GetMapping("/employe/{employeId}/statut/{statut}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<Imputation>> getImputationsByEmployeAndStatut(@PathVariable Long employeId, @PathVariable StatutImputation statut) {
        List<Imputation> imputations = imputationService.getImputationsByEmployeAndStatut(employeId, statut);
        return ResponseEntity.ok(imputations);
    }

    /**
     * Récupère les imputations avec id manager
     * Accessible par les managers
     */
    @GetMapping("/manager/{managerId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<Imputation>> getImputationsByManager(@PathVariable Long managerId) {
        List<Imputation> imputations = imputationService.getImputationsByManager(managerId);
        return ResponseEntity.ok(imputations);
    }

    /**
     * Récupère les imputations en attente de validation
     * Accessible par les managers
     */
    @GetMapping("/en-attente")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<Imputation>> getImputationsEnAttente(@RequestParam Long managerId) {
        List<Imputation> imputations = imputationService.getImputationsEnAttenteForManager(managerId);
        return ResponseEntity.ok(imputations);
    }

    /**
     * Met à jour une imputation
     * Accessible uniquement par l'employé propriétaire (modifications sur imputations en attente)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ResponseEntity<Imputation> updateImputation(
            @PathVariable Long id,
            @RequestParam Long employeId,
            @RequestBody Imputation updatedImputation) {
        Imputation updated = imputationService.updateImputation(id, employeId, updatedImputation);
        return ResponseEntity.ok(updated);
    }

    /**
     * Supprime une imputation
     * Accessible uniquement par l'employé propriétaire (suppression sur imputations en attente)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ResponseEntity<String> deleteImputation(
            @PathVariable Long id,
            @RequestParam Long employeId) {
        imputationService.deleteImputation(id, employeId);
        return ResponseEntity.ok("Imputation supprimée avec succès");
    }

    /**
     * Valide une imputation
     * Accessible uniquement par les managers
     */
    @PostMapping("/{imputationId}/valider")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Imputation> validerImputation(
            @PathVariable Long imputationId,
            @RequestParam Long managerId) {
        Imputation validated = imputationService.validerImputation(imputationId, managerId);
        return ResponseEntity.ok(validated);
    }

    /**
     * Rejette une imputation
     * Accessible uniquement par les managers
     */
    @PostMapping("/{imputationId}/rejeter")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Imputation> rejeterImputation(
            @PathVariable Long imputationId,
            @RequestParam Long managerId) {
        Imputation rejected = imputationService.rejeterImputation(imputationId, managerId);
        return ResponseEntity.ok(rejected);
    }
}


