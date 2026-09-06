package com.SSS.SGI.service;

import com.SSS.SGI.dto.AbsenceDTO;
import com.SSS.SGI.dto.AllouerQuotaRequest;
import com.SSS.SGI.dto.CreateAbsenceRequest;
import com.SSS.SGI.dto.QuotaAbsenceDTO;
import com.SSS.SGI.entity.Absence;
import com.SSS.SGI.entity.Collaborateur;
import com.SSS.SGI.entity.Employe;
import com.SSS.SGI.entity.Manager;
import com.SSS.SGI.entity.QuotaAbsence;
import com.SSS.SGI.entity.enums.StatutAbsence;
import com.SSS.SGI.entity.enums.TypeAbsence;
import com.SSS.SGI.exception.AbsenceChevauchementException;
import com.SSS.SGI.exception.AdminNonAutoriseException;
import com.SSS.SGI.exception.ManagerNonAutoriseException;
import com.SSS.SGI.exception.QuotaInsuffisantException;
import com.SSS.SGI.exception.ResourceNotFoundException;
import com.SSS.SGI.repository.AbsenceRepository;
import com.SSS.SGI.repository.CollaborateurRepository;
import com.SSS.SGI.repository.ManagerRepository;
import com.SSS.SGI.repository.QuotaAbsenceRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
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
    private final CollaborateurRepository collaborateurRepository;
    private final ManagerRepository managerRepository;

    @Value("${sgi.fichiers.dossier-justificatifs:./justificatifs}")
    private String dossierJustificatifs;

    /**
     * @param collaborateurId id d'un Employe ou d'un Manager : les deux peuvent déposer une
     *                        demande d'absence (le segment d'URL /employe/{id} est conservé
     *                        pour ne pas casser le contrat existant, mais accepte les deux).
     * @param actingAsAdmin   rôle ADMIN du demandeur, déterminé par le contrôleur à partir du
     *                        jeton. Un ADMIN est le sommet de la hiérarchie de validation :
     *                        il n'a pas d'approbateur, donc il ne dépose pas d'absence.
     */
    @Transactional
    public AbsenceDTO creerAbsence(Long collaborateurId, CreateAbsenceRequest request, boolean actingAsAdmin) {
        if (actingAsAdmin) {
            throw new AdminNonAutoriseException(
                    "Un administrateur ne peut pas déposer de demande d'absence : "
                            + "il valide les demandes des managers et n'a pas d'approbateur lui-même.");
        }

        Collaborateur collaborateur = collaborateurRepository.findById(collaborateurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collaborateur introuvable : " + collaborateurId));

        if (request.dateFin().isBefore(request.dateDebut())) {
            throw new IllegalArgumentException("La date de fin ne peut pas précéder la date de début.");
        }

        if (request.dateDebut().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Impossible de créer une absence dans le passé.");
        }

        long dureeJours = java.time.temporal.ChronoUnit.DAYS.between(request.dateDebut(), request.dateFin());
        if (dureeJours > 90) {
            throw new IllegalArgumentException("La durée d'une absence ne peut pas dépasser 90 jours.");
        }

        List<Absence> chevauchements = absenceRepository.findChevauchements(
                collaborateurId, request.dateDebut(), request.dateFin());
        if (!chevauchements.isEmpty()) {
            throw new AbsenceChevauchementException(
                    "Une absence existe déjà sur cette période pour ce collaborateur.");
        }

        double nombreJours = calculerJoursOuvres(request.dateDebut(), request.dateFin());
        TypeAbsence type = request.typeAbsence();

        if (type.isSoumisAQuota()) {
            QuotaAbsence quota = quotaAbsenceRepository
                    .findByCollaborateur_IdAndTypeAbsenceAndAnnee(collaborateurId, type, request.dateDebut().getYear())
                    .orElseThrow(() -> new QuotaInsuffisantException(
                            "Aucun quota " + type + " défini pour " + request.dateDebut().getYear()));
            if (quota.getJoursRestants() < nombreJours) {
                throw new QuotaInsuffisantException(
                        "Quota insuffisant : " + quota.getJoursRestants()
                                + " jour(s) restant(s) pour " + type + ", " + nombreJours + " demandé(s).");
            }
        }

        Absence absence = new Absence();
        absence.setCollaborateur(collaborateur);
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

    /**
     * @param managerId     id du valideur, dérivé du jeton par le contrôleur — jamais un id annoncé
     *                      par le client. Il sert aussi d'ancre au contrôle de périmètre : un
     *                      manager ne valide que les demandes de ses propres employés.
     * @param actingAsAdmin le rôle ADMIN du manager qui valide, déterminé par le contrôleur à
     *                       partir du jeton. Une demande déposée par un Manager ne peut être
     *                       validée que par un ADMIN, jamais par un manager pair.
     */
    @Transactional
    public AbsenceDTO validerAbsence(Long absenceId, Long managerId, boolean actingAsAdmin) {
        Absence absence = getOrThrow(absenceId);
        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager introuvable : " + managerId));

        verifierAutoriteValidation(absence, managerId, actingAsAdmin);

        absence.valider(manager);

        if (absence.getTypeAbsence().isSoumisAQuota()) {
            QuotaAbsence quota = quotaAbsenceRepository
                    .findByCollaborateur_IdAndTypeAbsenceAndAnnee(
                            absence.getCollaborateur().getId(),
                            absence.getTypeAbsence(),
                            absence.getDateDebut().getYear())
                    .orElseThrow(() -> new ResourceNotFoundException("Quota introuvable pour la validation."));

            // Revérification du solde au moment de la validation
            if (quota.getJoursRestants() < absence.getNombreJours()) {
                throw new QuotaInsuffisantException(
                        "Quota insuffisant au moment de la validation : " + quota.getJoursRestants()
                                + " jour(s) restant(s) pour " + absence.getTypeAbsence()
                                + ", " + absence.getNombreJours() + " demandé(s).");
            }

            quota.setJoursPris(quota.getJoursPris() + absence.getNombreJours());
            quotaAbsenceRepository.save(quota);
        }

        return toDTO(absenceRepository.save(absence));
    }

    @Transactional
    public AbsenceDTO rejeterAbsence(Long absenceId, Long managerId, String motif, boolean actingAsAdmin) {
        Absence absence = getOrThrow(absenceId);
        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager introuvable : " + managerId));

        verifierAutoriteValidation(absence, managerId, actingAsAdmin);

        absence.rejeter(manager, motif);
        return toDTO(absenceRepository.save(absence));
    }

    /**
     * Deux barrières distinctes :
     * — la demande d'un manager relève de l'ADMIN seul (un manager pair n'a pas autorité sur lui) ;
     * — la demande d'un employé relève de son propre manager, et de lui seul. Ce second contrôle
     *   se compare au valideur dérivé du jeton : sans lui, tout manager de l'entreprise validait
     *   les demandes de n'importe quel employé.
     *
     * Le cadrage du périmètre de l'ADMIN sur les demandes d'employés relève de la matrice des
     * droits et reste inchangé ici.
     */
    private void verifierAutoriteValidation(Absence absence, Long validateurId, boolean actingAsAdmin) {
        if (estManager(absence.getCollaborateur())) {
            if (!actingAsAdmin) {
                throw new ManagerNonAutoriseException(
                        "Seul un administrateur peut valider ou rejeter la demande d'absence d'un manager.");
            }
            return;
        }

        if (!actingAsAdmin && !appartientAUnEmployeDe(absence.getCollaborateur(), validateurId)) {
            throw new ManagerNonAutoriseException(
                    "Ce manager n'est pas autorisé à traiter les demandes d'absence de cet employé : "
                            + "l'employé n'appartient pas à son équipe.");
        }
    }

    /**
     * absence.collaborateur est chargé paresseusement et typé sur la classe racine
     * Collaborateur : un simple "instanceof Manager" sur le proxy Hibernate non résolu
     * échoue silencieusement (renvoie false même si la ligne est bien un manager).
     * Hibernate.unproxy force la résolution vers le type concret avant le test.
     */
    private boolean estManager(Collaborateur collaborateur) {
        return Hibernate.unproxy(collaborateur) instanceof Manager;
    }

    @Transactional
    public void annulerAbsence(Long absenceId, Long collaborateurId) {
        Absence absence = getOrThrow(absenceId);
        if (!absence.getCollaborateur().getId().equals(collaborateurId)) {
            throw new IllegalArgumentException("Cette absence n'appartient pas à ce collaborateur.");
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

    public List<AbsenceDTO> listerParEmploye(Long collaborateurId) {
        return absenceRepository.findByCollaborateur_Id(collaborateurId).stream().map(this::toDTO).toList();
    }

    /**
     * File de validation, scopée selon qui regarde :
     * - un ADMIN voit les demandes déposées par des managers ;
     * - un manager (non-admin) ne voit que les demandes de ses propres employés
     *   (jamais les siennes : une demande de manager n'est jamais une demande d'employé).
     */
    public List<AbsenceDTO> listerEnAttente(Long callerId, boolean actingAsAdmin) {
        return absenceRepository.findByStatut(StatutAbsence.EN_ATTENTE).stream()
                .filter(a -> actingAsAdmin
                        ? estManager(a.getCollaborateur())
                        : appartientAUnEmployeDe(a.getCollaborateur(), callerId))
                .map(this::toDTO)
                .toList();
    }

    private boolean appartientAUnEmployeDe(Collaborateur collaborateur, Long managerId) {
        Object reel = Hibernate.unproxy(collaborateur);
        if (!(reel instanceof Employe employe)) {
            return false;
        }
        return employe.getManager() != null && employe.getManager().getId().equals(managerId);
    }

    @Transactional
    public QuotaAbsenceDTO allouerQuota(AllouerQuotaRequest request) {
        Collaborateur collaborateur = collaborateurRepository.findById(request.employeId())
                .orElseThrow(() -> new ResourceNotFoundException("Collaborateur introuvable : " + request.employeId()));

        QuotaAbsence quota = quotaAbsenceRepository
                .findByCollaborateur_IdAndTypeAbsenceAndAnnee(request.employeId(), request.typeAbsence(), request.annee())
                .orElseGet(() -> {
                    QuotaAbsence nouveau = new QuotaAbsence();
                    nouveau.setCollaborateur(collaborateur);
                    nouveau.setTypeAbsence(request.typeAbsence());
                    nouveau.setAnnee(request.annee());
                    nouveau.setJoursPris(0.0);
                    return nouveau;
                });
        quota.setJoursAlloues(request.joursAlloues());
        return toDTO(quotaAbsenceRepository.save(quota));
    }

    public List<QuotaAbsenceDTO> getQuotas(Long collaborateurId, Integer annee) {
        return quotaAbsenceRepository.findByCollaborateur_IdAndAnnee(collaborateurId, annee)
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
                a.getCollaborateur().getId(),
                a.getCollaborateur().getNom() + " " + a.getCollaborateur().getPrenom(),
                estManager(a.getCollaborateur()) ? "MANAGER" : "EMPLOYE",
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
                q.getCollaborateur().getId(),
                q.getTypeAbsence(),
                q.getAnnee(),
                q.getJoursAlloues(),
                q.getJoursPris(),
                q.getJoursRestants()
        );
    }
}
