package com.SSS.SGI.controller;

import com.SSS.SGI.dto.AbsenceDTO;
import com.SSS.SGI.dto.AllouerQuotaRequest;
import com.SSS.SGI.dto.CreateAbsenceRequest;
import com.SSS.SGI.dto.QuotaAbsenceDTO;
import com.SSS.SGI.dto.RejeterAbsenceRequest;
import com.SSS.SGI.security.CustomUserDetails;
import com.SSS.SGI.security.IdentiteAppelant;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Les identifiants d'appelant présents dans les URL (employeId, managerId) sont conservés pour ne
 * pas rompre le contrat public, mais ne font plus autorité : ils sont confrontés au jeton via
 * {@link IdentiteAppelant} et c'est l'id du principal qui atteint le service.
 */
@RestController
@RequestMapping("/api/absences")
@RequiredArgsConstructor
@Tag(name = "Absences", description = "Demandes d'absence, validation, quotas et justificatifs")
public class AbsenceController {

    private final AbsenceService absenceService;

    @Operation(summary = "Créer une demande d'absence", description = "Accessible par l'employé ou le manager demandeur. "
            + "Un ADMIN ne peut pas déposer de demande : il n'a pas d'approbateur.")
    @ApiResponse(responseCode = "201", description = "Absence créée, statut EN_ATTENTE")
    @ApiResponse(responseCode = "400", description = "Dates invalides ou durée excessive")
    @ApiResponse(responseCode = "403", description = "Un administrateur ne peut pas déposer de demande d'absence")
    @ApiResponse(responseCode = "404", description = "Employé introuvable")
    @ApiResponse(responseCode = "409", description = "Chevauchement avec une absence existante")
    @PostMapping("/employe/{employeId}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER')")
    public ResponseEntity<AbsenceDTO> creer(
            @PathVariable Long employeId,
            @Valid @RequestBody CreateAbsenceRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(absenceService.creerAbsence(
                        IdentiteAppelant.resoudre(employeId, principal), request, estAdmin(principal)));
    }

    @Operation(summary = "Récupérer une absence par ID", description = "Accessible par employé, manager ou admin.")
    @ApiResponse(responseCode = "200", description = "Absence trouvée")
    @ApiResponse(responseCode = "404", description = "Absence introuvable")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<AbsenceDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(absenceService.getAbsence(id));
    }

    @Operation(summary = "Lister les absences d'un employé", description = "Accessible par employé, manager ou admin. "
            + "Un employé ne peut consulter que ses propres demandes.")
    @ApiResponse(responseCode = "200", description = "Liste des absences de l'employé")
    @ApiResponse(responseCode = "403", description = "Un employé consulte les demandes d'un autre collaborateur")
    @GetMapping("/employe/{employeId}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<AbsenceDTO>> listerParEmploye(
            @PathVariable Long employeId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        IdentiteAppelant.exigerProprietaireSiSimpleEmploye(employeId, principal);
        return ResponseEntity.ok(absenceService.listerParEmploye(employeId));
    }

    @Operation(summary = "Lister les absences en attente de validation", description = "Accessible par les managers. "
            + "Un ADMIN voit les demandes des managers ; un manager ne voit que celles de ses propres employés "
            + "(jamais les siennes).")
    @ApiResponse(responseCode = "200", description = "Liste des absences EN_ATTENTE visibles par l'appelant")
    @GetMapping("/en-attente")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<AbsenceDTO>> listerEnAttente(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(absenceService.listerEnAttente(principal.getId(), estAdmin(principal)));
    }

    @Operation(summary = "Téléverser le justificatif d'une absence", description = "Accessible par l'employé demandeur, "
            + "et par lui seul : le justificatif ne peut être déposé que sur sa propre demande.")
    @ApiResponse(responseCode = "200", description = "Justificatif enregistré")
    @ApiResponse(responseCode = "403", description = "L'absence ciblée appartient à un autre collaborateur")
    @ApiResponse(responseCode = "404", description = "Absence introuvable")
    @PostMapping(value = "/{id}/justificatif", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ResponseEntity<AbsenceDTO> uploaderJustificatif(@PathVariable Long id,
                                                            @Parameter(description = "Fichier justificatif (PDF, image, ...)")
                                                            @RequestParam("fichier") MultipartFile fichier,
                                                            @AuthenticationPrincipal CustomUserDetails principal) {
        // L'URL désigne l'absence, pas son titulaire : le lien de propriété n'est connu qu'en base.
        IdentiteAppelant.exigerProprietaire(absenceService.getAbsence(id).employeId(), principal);
        String url = absenceService.enregistrerFichierJustificatif(id, fichier);
        return ResponseEntity.ok(absenceService.ajouterJustificatif(id, url));
    }

    @Operation(summary = "Valider une absence", description = "Accessible par les managers. Décompte le quota si applicable. "
            + "Un manager ne traite que les demandes de ses propres employés ; une demande déposée par un "
            + "manager ne peut être validée que par un administrateur.")
    @ApiResponse(responseCode = "200", description = "Absence validée")
    @ApiResponse(responseCode = "403", description = "Demande hors de l'équipe du manager, ou demande d'un manager traitée par un non-administrateur")
    @ApiResponse(responseCode = "404", description = "Absence, manager ou quota introuvable")
    @ApiResponse(responseCode = "409", description = "Quota insuffisant ou justificatif manquant")
    @PostMapping("/{id}/valider")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<AbsenceDTO> valider(
            @PathVariable Long id,
            @RequestParam(required = false) Long managerId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(absenceService.validerAbsence(
                id, IdentiteAppelant.resoudre(managerId, principal), estAdmin(principal)));
    }

    @Operation(summary = "Rejeter une absence", description = "Accessible par les managers. "
            + "Un manager ne traite que les demandes de ses propres employés ; une demande déposée par un "
            + "manager ne peut être rejetée que par un administrateur.")
    @ApiResponse(responseCode = "200", description = "Absence rejetée")
    @ApiResponse(responseCode = "403", description = "Demande hors de l'équipe du manager, ou demande d'un manager traitée par un non-administrateur")
    @ApiResponse(responseCode = "404", description = "Absence ou manager introuvable")
    @PostMapping("/{id}/rejeter")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<AbsenceDTO> rejeter(
            @PathVariable Long id,
            @RequestParam(required = false) Long managerId,
            @Valid @RequestBody RejeterAbsenceRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(absenceService.rejeterAbsence(
                id, IdentiteAppelant.resoudre(managerId, principal), request.motif(), estAdmin(principal)));
    }

    @Operation(summary = "Annuler une absence", description = "Accessible par le collaborateur propriétaire (employé ou manager), uniquement si EN_ATTENTE.")
    @ApiResponse(responseCode = "204", description = "Absence annulée")
    @ApiResponse(responseCode = "400", description = "L'absence n'appartient pas à ce collaborateur")
    @ApiResponse(responseCode = "403", description = "`employeId` désigne un autre collaborateur que le porteur du jeton")
    @ApiResponse(responseCode = "409", description = "L'absence n'est plus EN_ATTENTE")
    @DeleteMapping("/{id}/employe/{employeId}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER')")
    public ResponseEntity<Void> annuler(
            @PathVariable Long id,
            @PathVariable Long employeId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        absenceService.annulerAbsence(id, IdentiteAppelant.resoudre(employeId, principal));
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

    @Operation(summary = "Lister les quotas d'un employé pour une année", description = "Accessible par employé, manager ou admin. "
            + "Un employé ne peut consulter que ses propres quotas.")
    @ApiResponse(responseCode = "200", description = "Liste des quotas de l'employé pour l'année donnée")
    @ApiResponse(responseCode = "403", description = "Un employé consulte les quotas d'un autre collaborateur")
    @GetMapping("/quotas/employe/{employeId}/annee/{annee}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<QuotaAbsenceDTO>> getQuotas(
            @PathVariable Long employeId,
            @PathVariable Integer annee,
            @AuthenticationPrincipal CustomUserDetails principal) {
        IdentiteAppelant.exigerProprietaireSiSimpleEmploye(employeId, principal);
        return ResponseEntity.ok(absenceService.getQuotas(employeId, annee));
    }

    private boolean estAdmin(CustomUserDetails principal) {
        return principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}