package com.SSS.SGI.controller;

import com.SSS.SGI.entity.Client;
import com.SSS.SGI.entity.Projet;
import com.SSS.SGI.entity.BudgetProjet;
import com.SSS.SGI.service.ProjetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

/**
 * Contrôleur pour gérer les clients et projets
 */
@RestController
@RequestMapping("/api/projets")
@CrossOrigin(origins = "*")
public class ProjetController {

    private final ProjetService projetService;

    public ProjetController(ProjetService projetService) {
        this.projetService = projetService;
    }

    // ==================== Gestion des Projets ====================

    /**
     * Crée un nouveau projet
     * Accessible par les managers
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER')")
    @SuppressWarnings("all")
    public ResponseEntity<Projet> createProjet(@Valid @RequestBody Projet projet) {
        Projet created = projetService.createProjet(projet);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Récupère un projet par son ID
     * Accessible par tous les collaborateurs
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER')")
    public ResponseEntity<Projet> getProjet(@PathVariable Long id) {
        Optional<Projet> projet = projetService.getProjetById(id);
        return projet.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Récupère tous les projets
     * Accessible par les managers
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<List<Projet>> getAllProjets() {
        List<Projet> projets = projetService.getAllProjets();
        return ResponseEntity.ok(projets);
    }

    /**
     * Récupère un projet par son nom
     * Accessible par les managers
     */
    @GetMapping("/nomProjet")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<Optional<Projet>> findProjetByNom(@RequestParam String nom) {
        Optional<Projet> projets = projetService.findProjetByNom(nom);
        return ResponseEntity.ok(projets);
    }

    /**
     * Récupère les projets d'un client
     * Accessible par les managers
     */
    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<List<Projet>> getProjetsByClient(@PathVariable Long clientId) {
        List<Projet> projets = projetService.getProjetsByClient(clientId);
        return ResponseEntity.ok(projets);
    }

    /**
     * Met à jour un projet
     * Accessible par les managers
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<Projet> updateProjet(
            @PathVariable Long id,
            @Valid @RequestBody Projet updatedProjet) {
        Projet updated = projetService.updateProjet(id, updatedProjet);
        return ResponseEntity.ok(updated);
    }

    /**
     * Supprime un projet
     * Accessible uniquement par les managers
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<String> deleteProjet(@PathVariable Long id) {
        projetService.deleteProjet(id);
        return ResponseEntity.ok("Projet supprimé avec succès");
    }

    // ==================== Gestion des Projets avec Budget ====================

    /**
     * Crée un projet avec budget
     * Accessible par les managers
     */
    @PostMapping("/budget")
    @PreAuthorize("hasAnyRole('MANAGER')")
    @SuppressWarnings("all")
    public ResponseEntity<BudgetProjet> createBudgetProjet(@Valid @RequestBody BudgetProjet budgetProjet) {
        BudgetProjet created = projetService.createBudgetProjet(budgetProjet);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Récupère un projet avec budget par son ID
     * Accessible par les managers et administrateurs
     */
    @GetMapping("/budget/{id}")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<BudgetProjet> getBudgetProjet(@PathVariable Long id) {
        Optional<BudgetProjet> budgetProjet = projetService.getBudgetProjetById(id);
        return budgetProjet.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Récupère tous les projets avec budget
     * Accessible par les managers et administrateurs
     */
    @GetMapping("/budget")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<List<BudgetProjet>> getAllBudgetProjets() {
        List<BudgetProjet> budgetProjets = projetService.getAllBudgetProjets();
        return ResponseEntity.ok(budgetProjets);
    }

    /**
     * Récupère les projets avec budget d'un client
     * Accessible par les managers et administrateurs
     */
    @GetMapping("/budget/client/{clientId}")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<List<BudgetProjet>> getBudgetProjetsByClient(@PathVariable Long clientId) {
        List<BudgetProjet> budgetProjets = projetService.getBudgetProjetsByClient(clientId);
        return ResponseEntity.ok(budgetProjets);
    }

    /**
     * Met à jour un projet avec budget
     * Accessible par les managers et administrateurs
     */
    @PutMapping("/budget/{id}")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<BudgetProjet> updateBudgetProjet(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBudgetProjetRequest request) {
        BudgetProjet updated = projetService.updateBudgetProjet(id, request.getBudgetInitial(), request.getTjm());
        return ResponseEntity.ok(updated);
    }

    /**
     * Supprime un projet avec budget
     * Accessible uniquement par les managers
     */
    @DeleteMapping("/budget/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<String> deleteBudgetProjet(@PathVariable Long id) {
        projetService.deleteBudgetProjet(id);
        return ResponseEntity.ok("Projet avec budget supprimé avec succès");
    }
}


