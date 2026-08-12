package com.SSS.SGI.controller;

import com.SSS.SGI.entity.Collaborateur;
import com.SSS.SGI.entity.Employe;
import com.SSS.SGI.entity.Manager;
import com.SSS.SGI.service.CollaborateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Collaborateurs", description = "Comptes employés et managers : CRUD, profil et mot de passe")
public class CollaborateurController {

    private final CollaborateurService collaborateurService;

    public CollaborateurController(CollaborateurService collaborateurService) {
        this.collaborateurService = collaborateurService;
    }

     @Operation(summary = "Récupérer un collaborateur par email", description = "Accessible uniquement par les managers.")
     @ApiResponse(responseCode = "200", description = "Collaborateur trouvé")
     @ApiResponse(responseCode = "404", description = "Collaborateur introuvable")
     @GetMapping("/email/{email}")
     @PreAuthorize("hasRole('MANAGER')")
     public ResponseEntity<Collaborateur> getCollaborateurByEmail(@PathVariable String email) {
        Optional<Collaborateur> collaborateur = collaborateurService.getCollaborateurByEmail(email);
        return collaborateur.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Rechercher un collaborateur par nom et prénom", description = "Accessible uniquement par les managers.")
    @ApiResponse(responseCode = "200", description = "Collaborateur trouvé")
    @ApiResponse(responseCode = "404", description = "Collaborateur introuvable")
    @GetMapping("/recherche")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Collaborateur> getCollaborateurByNomAndPrenom(
            @RequestParam String nom,
            @RequestParam String prenom) {
        Optional<Collaborateur> collaborateur = collaborateurService.getCollaborateurByNomAndPrenom(nom, prenom);
        return collaborateur.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

     @Operation(summary = "Lister tous les employés (simple)", description = "Accessible uniquement par les managers.")
     @ApiResponse(responseCode = "200", description = "Liste des employés")
     @GetMapping("/employe")
     @PreAuthorize("hasRole('MANAGER')")
     public ResponseEntity<List<Employe>> getAllEmployesSimple() {
         List<Employe> employes = collaborateurService.getAllEmployes();
         return ResponseEntity.ok(employes);
     }

     @Operation(summary = "Créer un employé", description = "Accessible uniquement par les managers.")
     @ApiResponse(responseCode = "201", description = "Employé créé")
     @PostMapping("/employe")
     @PreAuthorize("hasRole('MANAGER')")
     public ResponseEntity<Employe> createEmploye(@Valid @RequestBody Employe employe) {
         Employe created = collaborateurService.createEmploye(employe);
         return new ResponseEntity<>(created, HttpStatus.CREATED);
     }

     @Operation(summary = "Créer un manager", description = "Accessible uniquement par les managers.")
     @ApiResponse(responseCode = "201", description = "Manager créé")
     @PostMapping("/manager")
     @PreAuthorize("hasRole('MANAGER')")
     public ResponseEntity<Manager> createManager(@Valid @RequestBody Manager manager) {
         Manager created = collaborateurService.createManager(manager);
         return new ResponseEntity<>(created, HttpStatus.CREATED);
     }

     @Operation(summary = "Lister tous les managers (simple)", description = "Accessible uniquement par les managers.")
     @ApiResponse(responseCode = "200", description = "Liste des managers")
     @GetMapping("/manager")
     @PreAuthorize("hasRole('MANAGER')")
     public ResponseEntity<List<Manager>> getAllManagersSimple() {
         List<Manager> managers = collaborateurService.getAllManagers();
         return ResponseEntity.ok(managers);
     }

    @Operation(summary = "Récupérer le profil d'un collaborateur", description = "Accessible par tous les collaborateurs authentifiés.")
    @ApiResponse(responseCode = "200", description = "Collaborateur trouvé")
    @ApiResponse(responseCode = "404", description = "Collaborateur introuvable")
    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER')")
    public ResponseEntity<Collaborateur> getCollaborateur(@PathVariable Long id) {
        Optional<Collaborateur> collaborateur = collaborateurService.getCollaborateurById(id);
        return collaborateur.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Lister tous les collaborateurs", description = "Accessible uniquement par les managers.")
    @ApiResponse(responseCode = "200", description = "Liste de tous les collaborateurs")
    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<Collaborateur>> getAllCollaborateurs() {
        List<Collaborateur> collaborateurs = collaborateurService.getAllCollaborateurs();
        return ResponseEntity.ok(collaborateurs);
    }

    @Operation(
            summary = "Mettre à jour le profil (nom, prénom, email)",
            description = "Permet au collaborateur de modifier ses données. "
                    + "Nécessite `motDePasseActuel` : en l'absence de session/JWT, c'est la seule "
                    + "preuve de propriété du compte disponible actuellement (voir SECURITY.md).")
    @ApiResponse(responseCode = "200", description = "Profil mis à jour")
    @ApiResponse(responseCode = "400", description = "Mot de passe actuel incorrect ou collaborateur introuvable")
    @PutMapping("/{id:\\d+}/profile")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER')")
    public ResponseEntity<Collaborateur> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProfileRequest request) {
        Collaborateur updated = collaborateurService.updateCollaborateurProfile(
                id,
                request.getNom(),
                request.getPrenom(),
                request.getEmail(),
                request.getMotDePasseActuel()
        );
        return ResponseEntity.ok(updated);
    }

    @Operation(
            summary = "Changer le mot de passe",
            description = "Permet au collaborateur de modifier son mot de passe. Nécessite l'ancien mot de passe.")
    @ApiResponse(responseCode = "200", description = "Mot de passe changé avec succès")
    @ApiResponse(responseCode = "400", description = "Ancien mot de passe incorrect ou collaborateur introuvable")
    @PostMapping("/{id:\\d+}/change-password")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER')")
    public ResponseEntity<String> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest request) {
        collaborateurService.changePassword(id, request.getAncienMotDePasse(), request.getNouveauMotDePasse());
        return ResponseEntity.ok("Mot de passe changé avec succès");
    }

     @Operation(summary = "Lister tous les employés", description = "Accessible uniquement par les managers.")
     @ApiResponse(responseCode = "200", description = "Liste des employés")
     @GetMapping("/employes")
     @PreAuthorize("hasRole('MANAGER')")
     public ResponseEntity<List<Employe>> getAllEmployes() {
         List<Employe> employes = collaborateurService.getAllEmployes();
         return ResponseEntity.ok(employes);
     }

     @Operation(summary = "Récupérer un employé par ID", description = "Accessible uniquement par les managers.")
     @ApiResponse(responseCode = "200", description = "Employé trouvé")
     @ApiResponse(responseCode = "404", description = "Employé introuvable")
     @GetMapping("/employe/{id:\\d+}")
     @PreAuthorize("hasRole('MANAGER')")
     public ResponseEntity<Employe> getEmployeById(@PathVariable Long id) {
         Optional<Employe> employe = collaborateurService.getEmployeById(id);
         return employe.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
     }

     @Operation(summary = "Récupérer un employé par email", description = "Accessible uniquement par les managers.")
     @ApiResponse(responseCode = "200", description = "Employé trouvé")
     @ApiResponse(responseCode = "404", description = "Employé introuvable")
     @GetMapping("/employe/email/{email}")
     @PreAuthorize("hasRole('MANAGER')")
     public ResponseEntity<Employe> findEmployeByEmail(@PathVariable String email) {
         Optional<Employe> employe = collaborateurService.findEmployeByEmail(email);
         return employe.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
     }

     @Operation(summary = "Lister tous les managers", description = "Accessible uniquement par les managers.")
     @ApiResponse(responseCode = "200", description = "Liste des managers")
     @GetMapping("/managers")
     @PreAuthorize("hasRole('MANAGER')")
     public ResponseEntity<List<Manager>> getAllManagers() {
         List<Manager> managers = collaborateurService.getAllManagers();
         return ResponseEntity.ok(managers);
     }

     @Operation(summary = "Récupérer un manager par ID", description = "Accessible uniquement par les managers.")
     @ApiResponse(responseCode = "200", description = "Manager trouvé")
     @ApiResponse(responseCode = "404", description = "Manager introuvable")
     @GetMapping("/manager/{id:\\d+}")
     @PreAuthorize("hasRole('MANAGER')")
     public ResponseEntity<Manager> getManagerById(@PathVariable Long id) {
         Optional<Manager> manager = collaborateurService.getManagerById(id);
         return manager.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
     }

     @Operation(summary = "Récupérer un manager par email", description = "Accessible uniquement par les managers.")
     @ApiResponse(responseCode = "200", description = "Manager trouvé")
     @ApiResponse(responseCode = "404", description = "Manager introuvable")
     @GetMapping("/manager/email/{email}")
     @PreAuthorize("hasRole('MANAGER')")
     public ResponseEntity<Manager> getManagerByEmail(@PathVariable String email) {
         Optional<Manager> manager = collaborateurService.findByEmail(email);
         return manager.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
     }

    @Operation(summary = "Supprimer un collaborateur", description = "Accessible uniquement par les managers.")
    @ApiResponse(responseCode = "200", description = "Collaborateur supprimé")
    @DeleteMapping("/{id:\\d+}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<String> deleteCollaborateur(@PathVariable Long id) {
        collaborateurService.deleteCollaborateur(id);
        return ResponseEntity.ok("Collaborateur supprimé avec succès");
    }
}