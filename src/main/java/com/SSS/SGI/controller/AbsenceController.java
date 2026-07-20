package com.SSS.SGI.controller;

import com.SSS.SGI.dto.AbsenceDTO;
import com.SSS.SGI.dto.AllouerQuotaRequest;
import com.SSS.SGI.dto.CreateAbsenceRequest;
import com.SSS.SGI.dto.QuotaAbsenceDTO;
import com.SSS.SGI.dto.RejeterAbsenceRequest;
import com.SSS.SGI.service.AbsenceService;
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
public class AbsenceController {

    private final AbsenceService absenceService;

    @PostMapping("/employe/{employeId}")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ResponseEntity<AbsenceDTO> creer(@PathVariable Long employeId,
                                             @Valid @RequestBody CreateAbsenceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(absenceService.creerAbsence(employeId, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<AbsenceDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(absenceService.getAbsence(id));
    }

    @GetMapping("/employe/{employeId}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<AbsenceDTO>> listerParEmploye(@PathVariable Long employeId) {
        return ResponseEntity.ok(absenceService.listerParEmploye(employeId));
    }

    @GetMapping("/en-attente")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<AbsenceDTO>> listerEnAttente() {
        return ResponseEntity.ok(absenceService.listerEnAttente());
    }

    @PostMapping(value = "/{id}/justificatif", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ResponseEntity<AbsenceDTO> uploaderJustificatif(@PathVariable Long id,
                                                            @RequestParam("fichier") MultipartFile fichier) {
        String url = absenceService.enregistrerFichierJustificatif(id, fichier);
        return ResponseEntity.ok(absenceService.ajouterJustificatif(id, url));
    }

    @PostMapping("/{id}/valider")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<AbsenceDTO> valider(@PathVariable Long id, @RequestParam Long managerId) {
        return ResponseEntity.ok(absenceService.validerAbsence(id, managerId));
    }

    @PostMapping("/{id}/rejeter")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<AbsenceDTO> rejeter(@PathVariable Long id,
                                               @RequestParam Long managerId,
                                               @Valid @RequestBody RejeterAbsenceRequest request) {
        return ResponseEntity.ok(absenceService.rejeterAbsence(id, managerId, request.motif()));
    }

    @DeleteMapping("/{id}/employe/{employeId}")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ResponseEntity<Void> annuler(@PathVariable Long id, @PathVariable Long employeId) {
        absenceService.annulerAbsence(id, employeId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/quotas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QuotaAbsenceDTO> allouerQuota(@Valid @RequestBody AllouerQuotaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(absenceService.allouerQuota(request));
    }

    @GetMapping("/quotas/employe/{employeId}/annee/{annee}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<QuotaAbsenceDTO>> getQuotas(@PathVariable Long employeId, @PathVariable Integer annee) {
        return ResponseEntity.ok(absenceService.getQuotas(employeId, annee));
    }
}
