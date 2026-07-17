package com.SSS.SGI.controller;

import com.SSS.SGI.entity.Collaborateur;
import com.SSS.SGI.entity.Employe;
import com.SSS.SGI.entity.Manager;
import com.SSS.SGI.service.CollaborateurService;
import jakarta.validation.Valid;
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
@RequestMapping({"/api/collaborateurs", "/api/collaborateur"})
@CrossOrigin(origins = "*")
public class CollaborateurController {

    private final CollaborateurService collaborateurService;

    public CollaborateurController(CollaborateurService collaborateurService) {
        this.collaborateurService = collaborateurService;
    }

     /**
      * Récupère un collaborateur par son email
      * Accessible uniquement par les managers
      */
     @GetMapping("/email/{email}")
     @PreAuthorize("hasRole('MANAGER')")
     public ResponseEntity<Collaborateur> getCollaborateurByEmail(@PathVariable String email) {
        Optional<Collaborateur> collaborateur = collaborateurService.getCollaborateurByEmail(email);
        return collaborateur.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Récupère un collaborateur par son nom et prénom
     * Accessible uniquement par les managers
     */
    @GetMapping("/recherche")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Collaborateur> getCollaborateurByNomAndPrenom(
            @RequestParam String nom,
            @RequestParam String prenom) {
        Optional<Collaborateur> collaborateur = collaborateurService.getCollaborateurByNomAndPrenom(nom, prenom);
        return collaborateur.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
     /**
      * Récupère tous les employés
      * Accessible uniquement par les managers
      */
     @GetMapping("/employe")
     @PreAuthorize("hasRole('MANAGER')")
     public ResponseEntity<List<Employe>> getAllEmployesSimple() {
         List<Employe> employes = collaborateurService.getAllEmployes();
         return ResponseEntity.ok(employes);
     }

     /**
      * Crée un nouvel employé
      * Accessible uniquement par les managers
      */
     @PostMapping("/employe")
     @PreAuthorize("hasRole('MANAGER')")
     public ResponseEntity<Employe> createEmploye(@Valid @RequestBody Employe employe) {
         Employe created = collaborateurService.createEmploye(employe);
         return new ResponseEntity<>(created, HttpStatus.CREATED);
     }

     /**
      * Crée un nouveau manager
      * Accessible uniquement par les managers
      */
     @PostMapping("/manager")
     @PreAuthorize("hasRole('MANAGER')")
     public ResponseEntity<Manager> createManager(@Valid @RequestBody Manager manager) {
         Manager created = collaborateurService.createManager(manager);
         return new ResponseEntity<>(created, HttpStatus.CREATED);
     }

     /**
      * Récupère tous les managers
      * Accessible uniquement par les managers
      */
     @GetMapping("/manager")
     @PreAuthorize("hasRole('MANAGER')")
     public ResponseEntity<List<Manager>> getAllManagersSimple() {
         List<Manager> managers = collaborateurService.getAllManagers();
         return ResponseEntity.ok(managers);
     }

     /**
      * Récupère le profil du collaborateur courant
      * Accessible par tous les collaborateurs authentifiés
      * Note: Added regex \\d+ to ensure it only matches numerical IDs
      */
    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER')")
    public ResponseEntity<Collaborateur> getCollaborateur(@PathVariable Long id) {
        Optional<Collaborateur> collaborateur = collaborateurService.getCollaborateurById(id);
        return collaborateur.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Récupère tous les collaborateurs
     * Accessible uniquement par les managers
     */
    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<Collaborateur>> getAllCollaborateurs() {
        List<Collaborateur> collaborateurs = collaborateurService.getAllCollaborateurs();
        return ResponseEntity.ok(collaborateurs);
    }

    /**
     * Met à jour le profil du collaborateur (nom, prénom, email)
     * Permet au collaborateur de modifier ses données
     * Note: Added regex \\d+
     */
    @PutMapping("/{id:\\d+}/profile")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER')")
    public ResponseEntity<Collaborateur> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProfileRequest request) {
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
     * Note: Added regex \\d+
     */
    @PostMapping("/{id:\\d+}/change-password")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER')")
    public ResponseEntity<String> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest request) {
        collaborateurService.changePassword(id, request.getAncienMotDePasse(), request.getNouveauMotDePasse());
        return ResponseEntity.ok("Mot de passe changé avec succès");
    }

     /**
      * Récupère tous les employés
      * Accessible uniquement par les managers
      */
     @GetMapping("/employes")
     @PreAuthorize("hasRole('MANAGER')")
     public ResponseEntity<List<Employe>> getAllEmployes() {
         List<Employe> employes = collaborateurService.getAllEmployes();
         return ResponseEntity.ok(employes);
     }

     /**
      * Récupère un employé par son ID
      * Accessible uniquement par les managers
      * Note: Added regex \\d+
      */
     @GetMapping("/employe/{id:\\d+}")
     @PreAuthorize("hasRole('MANAGER')")
     public ResponseEntity<Employe> getEmployeById(@PathVariable Long id) {
         Optional<Employe> employe = collaborateurService.getEmployeById(id);
         return employe.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
     }

     /**
      * Récupère un employé par son email
      * Accessible uniquement par les managers
      */
     @GetMapping("/employe/email/{email}")
     @PreAuthorize("hasRole('MANAGER')")
     public ResponseEntity<Employe> findEmployeByEmail(@PathVariable String email) {
         Optional<Employe> employe = collaborateurService.findEmployeByEmail(email);
         return employe.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
     }

     /**
      * Récupère tous les managers
      * Accessible uniquement par les managers
      */
     @GetMapping("/managers")
     @PreAuthorize("hasRole('MANAGER')")
     public ResponseEntity<List<Manager>> getAllManagers() {
         List<Manager> managers = collaborateurService.getAllManagers();
         return ResponseEntity.ok(managers);
     }

     /**
      * Récupère un manager par son ID
      * Accessible uniquement par les managers
      * Note: Added regex \\d+
      */
     @GetMapping("/manager/{id:\\d+}")
     @PreAuthorize("hasRole('MANAGER')")
     public ResponseEntity<Manager> getManagerById(@PathVariable Long id) {
         Optional<Manager> manager = collaborateurService.getManagerById(id);
         return manager.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
     }

     /**
      * Récupère un manager par son email
      * Accessible uniquement par les managers
      */
     @GetMapping("/manager/email/{email}")
     @PreAuthorize("hasRole('MANAGER')")
     public ResponseEntity<Manager> getManagerByEmail(@PathVariable String email) {
         Optional<Manager> manager = collaborateurService.findByEmail(email);
         return manager.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
     }

    /**
     * Supprime un collaborateur
     * Accessible uniquement par les managers
     * Note: Added regex \\d+
     */
    @DeleteMapping("/{id:\\d+}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<String> deleteCollaborateur(@PathVariable Long id) {
        collaborateurService.deleteCollaborateur(id);
        return ResponseEntity.ok("Collaborateur supprimé avec succès");
    }
}


