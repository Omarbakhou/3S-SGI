package com.SSS.SGI.service;

import com.SSS.SGI.dto.AbsenceDTO;
import com.SSS.SGI.dto.AllouerQuotaRequest;
import com.SSS.SGI.dto.CreateAbsenceRequest;
import com.SSS.SGI.dto.QuotaAbsenceDTO;
import com.SSS.SGI.entity.Absence;
import com.SSS.SGI.entity.Employe;
import com.SSS.SGI.entity.Manager;
import com.SSS.SGI.entity.QuotaAbsence;
import com.SSS.SGI.entity.enums.StatutAbsence;
import com.SSS.SGI.entity.enums.TypeAbsence;
import com.SSS.SGI.exception.AbsenceChevauchementException;
import com.SSS.SGI.exception.QuotaInsuffisantException;
import com.SSS.SGI.exception.ResourceNotFoundException;
import com.SSS.SGI.repository.AbsenceRepository;
import com.SSS.SGI.repository.EmployeRepository;
import com.SSS.SGI.repository.ManagerRepository;
import com.SSS.SGI.repository.QuotaAbsenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AbsenceService {

    private final AbsenceRepository absenceRepository;
    private final QuotaAbsenceRepository quotaAbsenceRepository;
    private final EmployeRepository employeRepository;
    private final ManagerRepository managerRepository;

    @Value("${sgi.fichiers.dossier-justificatifs:./justificatifs}")
    private String dossierJustificatifs;

    @Transactional
    public AbsenceDTO creerAbsence(Long employeId, CreateAbsenceRequest request) {
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable : " + employeId));

        if (request.dateFin().isBefore(request.dateDebut())) {
            throw new IllegalArgumentException("La date de fin ne peut pas précéder la date de début.");
        }

        List<Absence> chevauchements = absenceRepository.findChevauchements(
                employeId, request.dateDebut(), request.dateFin());
        if (!chevauchements.isEmpty()) {
            throw new AbsenceChevauchementException(
                    "Une absence existe déjà sur cette période pour cet employé.");
        }

        double nombreJours = calculerJoursOuvres(request.dateDebut(), request.dateFin());
        TypeAbsence type = request.typeAbsence();

        if (type.isSoumisAQuota()) {
            QuotaAbsence quota = quotaAbsenceRepository
                    .findByEmploye_IdAndTypeAbsenceAndAnnee(employeId, type, request.dateDebut().getYear())
                    .orElseThrow(() -> new QuotaInsuffisantException(
                            "Aucun quota " + type + " défini pour " + request.dateDebut().getYear()));
            if (quota.getJoursRestants() < nombreJours) {
                throw new QuotaInsuffisantException(
                        "Quota insuffisant : " + quota.getJoursRestants()
                                + " jour(s) restant(s) pour " + type + ", " + nombreJours + " demandé(s).");
            }
        }

        Absence absence = new Absence();
        absence.setEmploye(employe);
        absence.setTypeAbsence(type);
        absence.setDateDebut(request.dateDebut());
        absence.setDateFin(request.dateFin());
        absence.setNombreJours(nombreJours);
        absence.setStatut(StatutAbsence.EN_ATTENTE);
        absence.setCommentaireEmploye(request.commentaireEmploye());

        return toDTO(absenceRepository.save(absence));
    }

    @Transactional
    public AbsenceDTO ajouterJustificatif(Long absenceId, String justificatifUrl) {
        Absence absence = getOrThrow(absenceId);
        absence.setJustificatifUrl(justificatifUrl);
        absence.setDateEnvoiJustificatif(LocalDate.now());
        return toDTO(absenceRepository.save(absence));
    }

    /** Stockage disque simplifié — à remplacer par S3/Blob storage en production. */
    public String enregistrerFichierJustificatif(Long absenceId, MultipartFile fichier) {
        try {
            Path dossier = Paths.get(dossierJustificatifs);
            Files.createDirectories(dossier);
            String nomFichier = absenceId + "_" + System.currentTimeMillis() + "_" + fichier.getOriginalFilename();
            Path cible = dossier.resolve(nomFichier);
            fichier.transferTo(cible);
            return cible.toString();
        } catch (IOException e) {
            throw new RuntimeException("Échec de l'enregistrement du justificatif.", e);
        }
    }

    @Transactional
    public AbsenceDTO validerAbsence(Long absenceId, Long managerId) {
        Absence absence = getOrThrow(absenceId);
        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager introuvable : " + managerId));

        absence.valider(manager);

        if (absence.getTypeAbsence().isSoumisAQuota()) {
            QuotaAbsence quota = quotaAbsenceRepository
                    .findByEmploye_IdAndTypeAbsenceAndAnnee(
                            absence.getEmploye().getId(),
                            absence.getTypeAbsence(),
                            absence.getDateDebut().getYear())
                    .orElseThrow(() -> new ResourceNotFoundException("Quota introuvable pour la validation."));
            quota.setJoursPris(quota.getJoursPris() + absence.getNombreJours());
            quotaAbsenceRepository.save(quota);
        }

        return toDTO(absenceRepository.save(absence));
    }

    @Transactional
    public AbsenceDTO rejeterAbsence(Long absenceId, Long managerId, String motif) {
        Absence absence = getOrThrow(absenceId);
        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager introuvable : " + managerId));
        absence.rejeter(manager, motif);
        return toDTO(absenceRepository.save(absence));
    }

    @Transactional
    public void annulerAbsence(Long absenceId, Long employeId) {
        Absence absence = getOrThrow(absenceId);
        if (!absence.getEmploye().getId().equals(employeId)) {
            throw new IllegalArgumentException("Cette absence n'appartient pas à cet employé.");
        }
        if (absence.getStatut() != StatutAbsence.EN_ATTENTE) {
            throw new IllegalStateException("Seule une absence EN_ATTENTE peut être annulée.");
        }
        absence.setStatut(StatutAbsence.ANNULEE);
        absenceRepository.save(absence);
    }

    public AbsenceDTO getAbsence(Long id) {
        return toDTO(getOrThrow(id));
    }

    public List<AbsenceDTO> listerParEmploye(Long employeId) {
        return absenceRepository.findByEmploye_Id(employeId).stream().map(this::toDTO).toList();
    }

    public List<AbsenceDTO> listerEnAttente() {
        return absenceRepository.findByStatut(StatutAbsence.EN_ATTENTE).stream().map(this::toDTO).toList();
    }

    @Transactional
    public QuotaAbsenceDTO allouerQuota(AllouerQuotaRequest request) {
        Employe employe = employeRepository.findById(request.employeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable : " + request.employeId()));

        QuotaAbsence quota = quotaAbsenceRepository
                .findByEmploye_IdAndTypeAbsenceAndAnnee(request.employeId(), request.typeAbsence(), request.annee())
                .orElseGet(() -> {
                    QuotaAbsence nouveau = new QuotaAbsence();
                    nouveau.setEmploye(employe);
                    nouveau.setTypeAbsence(request.typeAbsence());
                    nouveau.setAnnee(request.annee());
                    nouveau.setJoursPris(0.0);
                    return nouveau;
                });
        quota.setJoursAlloues(request.joursAlloues());
        return toDTO(quotaAbsenceRepository.save(quota));
    }

    public List<QuotaAbsenceDTO> getQuotas(Long employeId, Integer annee) {
        return quotaAbsenceRepository.findByEmploye_IdAndAnnee(employeId, annee)
                .stream().map(this::toDTO).toList();
    }

    /**
     * Jours ouvrés (lun-ven) entre deux dates incluses.
     * Ne tient pas encore compte des jours fériés : à raffiner avec une
     * table JourFerie si le besoin se confirme.
     */
    private double calculerJoursOuvres(LocalDate debut, LocalDate fin) {
        double jours = 0;
        LocalDate courant = debut;
        while (!courant.isAfter(fin)) {
            DayOfWeek jourSemaine = courant.getDayOfWeek();
            if (jourSemaine != DayOfWeek.SATURDAY && jourSemaine != DayOfWeek.SUNDAY) {
                jours++;
            }
            courant = courant.plusDays(1);
        }
        return jours;
    }

    private Absence getOrThrow(Long id) {
        return absenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Absence introuvable : " + id));
    }

    private AbsenceDTO toDTO(Absence a) {
        return new AbsenceDTO(
                a.getId(),
                a.getTypeAbsence(),
                a.getDateDebut(),
                a.getDateFin(),
                a.getNombreJours(),
                a.getStatut(),
                a.getEmploye().getId(),
                a.getEmploye().getNom() + " " + a.getEmploye().getPrenom(),
                a.getManagerValidateur() != null ? a.getManagerValidateur().getId() : null,
                a.getDateDemande(),
                a.getDateValidation(),
                a.getCommentaireEmploye(),
                a.getMotifRejet(),
                a.getJustificatifUrl(),
                a.getTypeAbsence().isJustificatifObligatoire()
        );
    }

    private QuotaAbsenceDTO toDTO(QuotaAbsence q) {
        return new QuotaAbsenceDTO(
                q.getId(),
                q.getEmploye().getId(),
                q.getTypeAbsence(),
                q.getAnnee(),
                q.getJoursAlloues(),
                q.getJoursPris(),
                q.getJoursRestants()
        );
    }
}
