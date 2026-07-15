package com.SSS.SGI.controller;

import com.SSS.SGI.entity.Collaborateur;
import com.SSS.SGI.entity.Employe;
import com.SSS.SGI.entity.Manager;
import com.SSS.SGI.service.CollaborateurService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Contrôleur pour gérer les collaborateurs
 * Permet le CRUD sur le compte collaborateur
 */
@RestController
@RequestMapping("/api/collaborateurs")
@CrossOrigin(origins = "*")
public class CollaborateurController {

    private final CollaborateurService collaborateurService;

    public CollaborateurController(CollaborateurService collaborateurService) {
        this.collaborateurService = collaborateurService;
    }

    /**
     * Crée un nouvel employé
     * Accessible uniquement par les administrateurs
     */
    @PostMapping("/employe")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Employe> createEmploye(@RequestBody Employe employe) {
        Employe created = collaborateurService.createEmploye(employe);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Crée un nouveau manager
     * Accessible uniquement par les administrateurs
     */
    @PostMapping("/manager")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Manager> createManager(@RequestBody Manager manager) {
        Manager created = collaborateurService.createManager(manager);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Récupère le profil du collaborateur courant
     * Accessible par tous les collaborateurs authentifiés
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER')")
    public ResponseEntity<Collaborateur> getCollaborateur(@PathVariable Long id) {
        Optional<Collaborateur> collaborateur = collaborateurService.getCollaborateurById(id);
        return collaborateur.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Récupère tous les collaborateurs
     * Accessible uniquement par les administrateurs
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Collaborateur>> getAllCollaborateurs() {
        List<Collaborateur> collaborateurs = collaborateurService.getAllCollaborateurs();
        return ResponseEntity.ok(collaborateurs);
    }

    /**
     * Met à jour le profil du collaborateur (nom, prénom, email)
     * Permet au collaborateur de modifier ses données
     */
    @PutMapping("/{id}/profile")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER')")
    public ResponseEntity<Collaborateur> updateProfile(
            @PathVariable Long id,
            @RequestBody UpdateProfileRequest request) {
        Collaborateur updated = collaborateurService.updateCollaborateurProfile(
            id,
            request.getNom(),
            request.getPrenom(),
            request.getEmail()
        );
        return ResponseEntity.ok(updated);
    }

    /**
     * Change le mot de passe du collaborateur
     * Permet au collaborateur de modifier son mot de passe
     */
    @PostMapping("/{id}/change-password")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER')")
    public ResponseEntity<String> changePassword(
            @PathVariable Long id,
            @RequestBody ChangePasswordRequest request) {
        collaborateurService.changePassword(id, request.getAncienMotDePasse(), request.getNouveauMotDePasse());
        return ResponseEntity.ok("Mot de passe changé avec succès");
    }

    /**
     * Récupère tous les employés
     * Accessible uniquement par les administrateurs
     */
    @GetMapping("/employes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Employe>> getAllEmployes() {
        List<Employe> employes = collaborateurService.getAllEmployes();
        return ResponseEntity.ok(employes);
    }

    /**
     * Récupère tous les managers
     * Accessible uniquement par les administrateurs
     */
    @GetMapping("/managers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Manager>> getAllManagers() {
        List<Manager> managers = collaborateurService.getAllManagers();
        return ResponseEntity.ok(managers);
    }

    /**
     * Supprime un collaborateur
     * Accessible uniquement par les administrateurs
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteCollaborateur(@PathVariable Long id) {
        collaborateurService.deleteCollaborateur(id);
        return ResponseEntity.ok("Collaborateur supprimé avec succès");
    }
}


