package com.SSS.SGI.controller;

import com.SSS.SGI.entity.Client;
import com.SSS.SGI.entity.Projet;
import com.SSS.SGI.entity.BudgetProjet;
import com.SSS.SGI.service.ProjetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Projets", description = "Gestion des projets, avec ou sans budget forfaitaire")
public class ProjetController {

    private final ProjetService projetService;

    public ProjetController(ProjetService projetService) {
        this.projetService = projetService;
    }

    // ==================== Gestion des Projets ====================

    @Operation(summary = "Créer un projet", description = "Accessible par les managers.")
    @ApiResponse(responseCode = "201", description = "Projet créé")
    @ApiResponse(responseCode = "400", description = "Client manquant")
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER')")
    @SuppressWarnings("all")
    public ResponseEntity<Projet> createProjet(@Valid @RequestBody Projet projet) {
        Projet created = projetService.createProjet(projet);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @Operation(summary = "Récupérer un projet par ID", description = "Accessible par tous les collaborateurs.")
    @ApiResponse(responseCode = "200", description = "Projet trouvé")
    @ApiResponse(responseCode = "404", description = "Projet introuvable")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER')")
    public ResponseEntity<Projet> getProjet(@PathVariable Long id) {
        Optional<Projet> projet = projetService.getProjetById(id);
        return projet.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Lister tous les projets", description = "Accessible par les managers.")
    @ApiResponse(responseCode = "200", description = "Liste des projets")
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<List<Projet>> getAllProjets() {
        List<Projet> projets = projetService.getAllProjets();
        return ResponseEntity.ok(projets);
    }

    @Operation(summary = "Récupérer un projet par son nom", description = "Accessible par les managers.")
    @ApiResponse(responseCode = "200", description = "Projet trouvé (ou vide si absent)")
    @GetMapping("/nomProjet")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<Optional<Projet>> findProjetByNom(@RequestParam String nom) {
        Optional<Projet> projets = projetService.findProjetByNom(nom);
        return ResponseEntity.ok(projets);
    }

    @Operation(summary = "Lister les projets d'un client", description = "Accessible par les managers.")
    @ApiResponse(responseCode = "200", description = "Liste des projets du client")
    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<List<Projet>> getProjetsByClient(@PathVariable Long clientId) {
        List<Projet> projets = projetService.getProjetsByClient(clientId);
        return ResponseEntity.ok(projets);
    }

    @Operation(summary = "Mettre à jour un projet", description = "Accessible par les managers.")
    @ApiResponse(responseCode = "200", description = "Projet mis à jour")
    @ApiResponse(responseCode = "400", description = "Projet introuvable")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<Projet> updateProjet(
            @PathVariable Long id,
            @Valid @RequestBody Projet updatedProjet) {
        Projet updated = projetService.updateProjet(id, updatedProjet);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Supprimer un projet", description = "Accessible uniquement par les managers.")
    @ApiResponse(responseCode = "200", description = "Projet supprimé")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<String> deleteProjet(@PathVariable Long id) {
        projetService.deleteProjet(id);
        return ResponseEntity.ok("Projet supprimé avec succès");
    }

    // ==================== Gestion des Projets avec Budget ====================

    @Operation(summary = "Créer un projet avec budget", description = "Accessible par les managers.")
    @ApiResponse(responseCode = "201", description = "Projet budgétisé créé")
    @ApiResponse(responseCode = "400", description = "Client, budget initial ou TJM manquant")
    @PostMapping("/budget")
    @PreAuthorize("hasAnyRole('MANAGER')")
    @SuppressWarnings("all")
    public ResponseEntity<BudgetProjet> createBudgetProjet(@Valid @RequestBody BudgetProjet budgetProjet) {
        BudgetProjet created = projetService.createBudgetProjet(budgetProjet);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @Operation(summary = "Récupérer un projet avec budget par ID", description = "Accessible par les managers et administrateurs.")
    @ApiResponse(responseCode = "200", description = "Projet budgétisé trouvé")
    @ApiResponse(responseCode = "404", description = "Projet budgétisé introuvable")
    @GetMapping("/budget/{id}")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<BudgetProjet> getBudgetProjet(@PathVariable Long id) {
        Optional<BudgetProjet> budgetProjet = projetService.getBudgetProjetById(id);
        return budgetProjet.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Lister tous les projets avec budget", description = "Accessible par les managers et administrateurs.")
    @ApiResponse(responseCode = "200", description = "Liste des projets budgétisés")
    @GetMapping("/budget")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<List<BudgetProjet>> getAllBudgetProjets() {
        List<BudgetProjet> budgetProjets = projetService.getAllBudgetProjets();
        return ResponseEntity.ok(budgetProjets);
    }

    @Operation(summary = "Lister les projets avec budget d'un client", description = "Accessible par les managers et administrateurs.")
    @ApiResponse(responseCode = "200", description = "Liste des projets budgétisés du client")
    @GetMapping("/budget/client/{clientId}")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<List<BudgetProjet>> getBudgetProjetsByClient(@PathVariable Long clientId) {
        List<BudgetProjet> budgetProjets = projetService.getBudgetProjetsByClient(clientId);
        return ResponseEntity.ok(budgetProjets);
    }

    @Operation(summary = "Mettre à jour le budget d'un projet", description = "Accessible par les managers et administrateurs.")
    @ApiResponse(responseCode = "200", description = "Projet budgétisé mis à jour")
    @ApiResponse(responseCode = "400", description = "Projet budgétisé introuvable")
    @PutMapping("/budget/{id}")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<BudgetProjet> updateBudgetProjet(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBudgetProjetRequest request) {
        BudgetProjet updated = projetService.updateBudgetProjet(id, request.getBudgetInitial(), request.getTjm());
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Supprimer un projet avec budget", description = "Accessible uniquement par les managers.")
    @ApiResponse(responseCode = "200", description = "Projet budgétisé supprimé")
    @DeleteMapping("/budget/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<String> deleteBudgetProjet(@PathVariable Long id) {
        projetService.deleteBudgetProjet(id);
        return ResponseEntity.ok("Projet avec budget supprimé avec succès");
    }
}