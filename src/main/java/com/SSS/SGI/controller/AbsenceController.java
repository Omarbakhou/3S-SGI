package com.SSS.SGI.controller;

import com.SSS.SGI.dto.AbsenceDTO;
import com.SSS.SGI.dto.AllouerQuotaRequest;
import com.SSS.SGI.dto.CreateAbsenceRequest;
import com.SSS.SGI.dto.QuotaAbsenceDTO;
import com.SSS.SGI.dto.RejeterAbsenceRequest;
import com.SSS.SGI.service.AbsenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/absences")
@RequiredArgsConstructor
@Tag(name = "Absences", description = "Demandes d'absence, validation, quotas et justificatifs")
public class AbsenceController {

    private final AbsenceService absenceService;

    @Operation(summary = "Créer une demande d'absence", description = "Accessible par l'employé demandeur.")
    @ApiResponse(responseCode = "201", description = "Absence créée, statut EN_ATTENTE")
    @ApiResponse(responseCode = "400", description = "Dates invalides ou durée excessive")
    @ApiResponse(responseCode = "404", description = "Employé introuvable")
    @ApiResponse(responseCode = "409", description = "Chevauchement avec une absence existante")
    @PostMapping("/employe/{employeId}")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ResponseEntity<AbsenceDTO> creer(@PathVariable Long employeId,
                                             @Valid @RequestBody CreateAbsenceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(absenceService.creerAbsence(employeId, request));
    }

    @Operation(summary = "Récupérer une absence par ID", description = "Accessible par employé, manager ou admin.")
    @ApiResponse(responseCode = "200", description = "Absence trouvée")
    @ApiResponse(responseCode = "404", description = "Absence introuvable")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<AbsenceDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(absenceService.getAbsence(id));
    }

    @Operation(summary = "Lister les absences d'un employé", description = "Accessible par employé, manager ou admin.")
    @ApiResponse(responseCode = "200", description = "Liste des absences de l'employé")
    @GetMapping("/employe/{employeId}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<AbsenceDTO>> listerParEmploye(@PathVariable Long employeId) {
        return ResponseEntity.ok(absenceService.listerParEmploye(employeId));
    }

    @Operation(summary = "Lister les absences en attente de validation", description = "Accessible par les managers.")
    @ApiResponse(responseCode = "200", description = "Liste des absences EN_ATTENTE")
    @GetMapping("/en-attente")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<AbsenceDTO>> listerEnAttente() {
        return ResponseEntity.ok(absenceService.listerEnAttente());
    }

    @Operation(summary = "Téléverser le justificatif d'une absence", description = "Accessible par l'employé demandeur.")
    @ApiResponse(responseCode = "200", description = "Justificatif enregistré")
    @ApiResponse(responseCode = "404", description = "Absence introuvable")
    @PostMapping(value = "/{id}/justificatif", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ResponseEntity<AbsenceDTO> uploaderJustificatif(@PathVariable Long id,
                                                            @Parameter(description = "Fichier justificatif (PDF, image, ...)")
                                                            @RequestParam("fichier") MultipartFile fichier) {
        String url = absenceService.enregistrerFichierJustificatif(id, fichier);
        return ResponseEntity.ok(absenceService.ajouterJustificatif(id, url));
    }

    @Operation(summary = "Valider une absence", description = "Accessible par les managers. Décompte le quota si applicable.")
    @ApiResponse(responseCode = "200", description = "Absence validée")
    @ApiResponse(responseCode = "404", description = "Absence, manager ou quota introuvable")
    @ApiResponse(responseCode = "409", description = "Quota insuffisant ou justificatif manquant")
    @PostMapping("/{id}/valider")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<AbsenceDTO> valider(@PathVariable Long id, @RequestParam Long managerId) {
        return ResponseEntity.ok(absenceService.validerAbsence(id, managerId));
    }

    @Operation(summary = "Rejeter une absence", description = "Accessible par les managers.")
    @ApiResponse(responseCode = "200", description = "Absence rejetée")
    @ApiResponse(responseCode = "404", description = "Absence ou manager introuvable")
    @PostMapping("/{id}/rejeter")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<AbsenceDTO> rejeter(@PathVariable Long id,
                                               @RequestParam Long managerId,
                                               @Valid @RequestBody RejeterAbsenceRequest request) {
        return ResponseEntity.ok(absenceService.rejeterAbsence(id, managerId, request.motif()));
    }

    @Operation(summary = "Annuler une absence", description = "Accessible par l'employé propriétaire, uniquement si EN_ATTENTE.")
    @ApiResponse(responseCode = "204", description = "Absence annulée")
    @ApiResponse(responseCode = "400", description = "L'absence n'appartient pas à cet employé")
    @ApiResponse(responseCode = "409", description = "L'absence n'est plus EN_ATTENTE")
    @DeleteMapping("/{id}/employe/{employeId}")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ResponseEntity<Void> annuler(@PathVariable Long id, @PathVariable Long employeId) {
        absenceService.annulerAbsence(id, employeId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Allouer un quota d'absence à un employé", description = "Accessible par les administrateurs.")
    @ApiResponse(responseCode = "201", description = "Quota créé ou mis à jour")
    @ApiResponse(responseCode = "404", description = "Employé introuvable")
    @PostMapping("/quotas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QuotaAbsenceDTO> allouerQuota(@Valid @RequestBody AllouerQuotaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(absenceService.allouerQuota(request));
    }

    @Operation(summary = "Lister les quotas d'un employé pour une année", description = "Accessible par employé, manager ou admin.")
    @ApiResponse(responseCode = "200", description = "Liste des quotas de l'employé pour l'année donnée")
    @GetMapping("/quotas/employe/{employeId}/annee/{annee}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<QuotaAbsenceDTO>> getQuotas(@PathVariable Long employeId, @PathVariable Integer annee) {
        return ResponseEntity.ok(absenceService.getQuotas(employeId, annee));
    }
}