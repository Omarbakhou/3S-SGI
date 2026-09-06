package com.SSS.SGI.service;

import com.SSS.SGI.dto.CreateImputationRequest;
import com.SSS.SGI.dto.ImputationDTO;
import com.SSS.SGI.entity.Employe;
import com.SSS.SGI.entity.Imputation;
import com.SSS.SGI.entity.Manager;
import com.SSS.SGI.entity.Projet;
import com.SSS.SGI.entity.StatutImputation;
import com.SSS.SGI.exception.ImputationNonAutoriseeException;
import com.SSS.SGI.exception.ManagerNonAutoriseException;
import com.SSS.SGI.exception.ResourceNotFoundException;
import com.SSS.SGI.exception.ValidationException;
import com.SSS.SGI.repository.AffectationRepository;
import com.SSS.SGI.repository.EmployeRepository;
import com.SSS.SGI.repository.ImputationRepository;
import com.SSS.SGI.repository.ManagerRepository;
import com.SSS.SGI.repository.ProjetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class ImputationService {

    private final ImputationRepository imputationRepository;
    private final EmployeRepository employeRepository;
    private final ManagerRepository managerRepository;
    private final ProjetRepository projetRepository;
    private final AffectationRepository affectationRepository;

    public ImputationService(
            ImputationRepository imputationRepository,
            EmployeRepository employeRepository,
            ManagerRepository managerRepository,
            ProjetRepository projetRepository,
            AffectationRepository affectationRepository) {
        this.imputationRepository = imputationRepository;
        this.employeRepository = employeRepository;
        this.managerRepository = managerRepository;
        this.projetRepository = projetRepository;
        this.affectationRepository = affectationRepository;
    }

    @Transactional
    public ImputationDTO creerImputation(Long employeId, CreateImputationRequest request) {
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable : " + employeId));
        Projet projet = projetRepository.findById(request.projetId())
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable : " + request.projetId()));

        validerReglesMetier(employe, projet, request.dateImputation(), request.heures(), null);

        Imputation imputation = new Imputation();
        imputation.setNom(request.nom());
        imputation.setDateImputation(request.dateImputation());
        imputation.setHeures(request.heures());
        imputation.setProjet(projet);
        imputation.setEmploye(employe);
        imputation.setStatut(StatutImputation.EN_ATTENTE);

        return toDTO(imputationRepository.save(imputation));
    }

    public ImputationDTO getImputation(Long id) {
        return toDTO(getOrThrow(id));
    }

    public List<ImputationDTO> getAllImputations() {
        return imputationRepository.findAll().stream().map(this::toDTO).toList();
    }

    public List<ImputationDTO> getImputationsByEmploye(Long employeId) {
        return imputationRepository.findByEmployeId(employeId).stream().map(this::toDTO).toList();
    }

    public List<ImputationDTO> getImputationsByProjet(Long projetId) {
        return imputationRepository.findByProjetId(projetId).stream().map(this::toDTO).toList();
    }

    public List<ImputationDTO> getImputationsByEmployeIdAndProjetId(Long employeId, Long projetId) {
        return imputationRepository.findByEmployeIdAndProjetId(employeId, projetId).stream().map(this::toDTO).toList();
    }

    public List<ImputationDTO> getImputationsByStatut(StatutImputation statut) {
        return imputationRepository.findByStatut(statut).stream().map(this::toDTO).toList();
    }

    public List<ImputationDTO> getImputationsByManager(Long managerId) {
        return imputationRepository.findByManagerValidateurId(managerId).stream().map(this::toDTO).toList();
    }

    public List<ImputationDTO> getImputationsByEmployeAndStatut(Long employeId, StatutImputation statut) {
        return imputationRepository.findByEmployeIdAndStatut(employeId, statut).stream().map(this::toDTO).toList();
    }

    /**
     * Met à jour une imputation (employé)
     * L'employé ne peut modifier que ses propres imputations, en attente uniquement
     */
    @Transactional
    public ImputationDTO updateImputation(Long id, Long employeId, CreateImputationRequest request) {
        Imputation i = getOrThrow(id);
        if (!i.getEmploye().getId().equals(employeId)) {
            throw new ImputationNonAutoriseeException("Cette imputation n'appartient pas à cet employé.");
        }
        if (i.getStatut() != StatutImputation.EN_ATTENTE) {
            throw new IllegalStateException("Seule une imputation en attente peut être modifiée");
        }
        Projet projet = projetRepository.findById(request.projetId())
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable : " + request.projetId()));

        validerReglesMetier(i.getEmploye(), projet, request.dateImputation(), request.heures(), id);

        i.setNom(request.nom());
        i.setDateImputation(request.dateImputation());
        i.setHeures(request.heures());
        i.setProjet(projet);

        return toDTO(imputationRepository.save(i));
    }

    /**
     * Supprime une imputation (employé)
     * L'employé ne peut supprimer que ses propres imputations, en attente uniquement
     */
    @Transactional
    public void deleteImputation(Long id, Long employeId) {
        Imputation i = getOrThrow(id);
        if (!i.getEmploye().getId().equals(employeId)) {
            throw new ImputationNonAutoriseeException("Cette imputation n'appartient pas à cet employé.");
        }
        if (i.getStatut() != StatutImputation.EN_ATTENTE) {
            throw new IllegalStateException("Seule une imputation en attente peut être supprimée");
        }
        imputationRepository.deleteById(id);
    }

    /**
     * Valide une imputation (manager)
     * Le manager doit être celui de l'employé concerné
     */
    @Transactional
    public ImputationDTO validerImputation(Long imputationId, Long managerId) {
        Imputation i = getOrThrow(imputationId);
        Manager m = managerRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager introuvable : " + managerId));

        verifierLegitimiteManager(i, managerId);

        i.valider(m);
        return toDTO(imputationRepository.save(i));
    }

    /**
     * Rejette une imputation (manager)
     * Le manager doit être celui de l'employé concerné
     */
    @Transactional
    public ImputationDTO rejeterImputation(Long imputationId, Long managerId, String motif) {
        Imputation i = getOrThrow(imputationId);
        Manager m = managerRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager introuvable : " + managerId));

        verifierLegitimiteManager(i, managerId);

        i.rejeter(m, motif);
        return toDTO(imputationRepository.save(i));
    }

    /**
     * Récupère les imputations en attente de validation pour un manager précis
     * (uniquement celles de ses propres employés)
     */
    public List<ImputationDTO> getImputationsEnAttenteForManager(Long managerId) {
        return imputationRepository.findByStatut(StatutImputation.EN_ATTENTE).stream()
                .filter(i -> i.getEmploye().getManager() != null
                        && i.getEmploye().getManager().getId().equals(managerId))
                .map(this::toDTO)
                .toList();
    }

    /**
     * Cumul des heures validées par projet — la finalité du système d'imputations.
     */
    public Double getCumulHeuresValideesByProjet(Long projetId) {
        projetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable : " + projetId));
        return imputationRepository.sumHeuresValideesByProjet(projetId);
    }

    private void verifierLegitimiteManager(Imputation imputation, Long managerId) {
        if (imputation.getEmploye().getManager() == null
                || !imputation.getEmploye().getManager().getId().equals(managerId)) {
            throw new ManagerNonAutoriseException(
                    "Ce manager n'est pas autorisé à valider les imputations de cet employé.");
        }
    }

    /**
     * Règles métier appliquées à la création ET à la modification d'une imputation.
     * excludeImputationId permet, en modification, d'ignorer l'imputation elle-même
     * dans le contrôle anti-doublon.
     */
    private void validerReglesMetier(Employe employe, Projet projet, LocalDate date, Double heures, Long excludeImputationId) {
        if (heures <= 0 || heures > 24) {
            throw new ValidationException(
                    "Le nombre d'heures doit être strictement supérieur à 0 et inférieur ou égal à 24 pour une même journée : "
                            + heures + " heure(s) demandée(s).");
        }

        if (date.isAfter(LocalDate.now().plusYears(1))) {
            throw new ValidationException(
                    "La date de l'imputation ne peut pas être postérieure de plus d'un an à aujourd'hui : " + date + ".");
        }

        double heuresDejaImputees = imputationRepository.sumHeuresDuJour(employe.getId(), date, excludeImputationId);
        double totalJournalier = heuresDejaImputees + heures;
        if (totalJournalier > 8) {
            throw new ValidationException(
                    "Plafond journalier dépassé pour le " + date + " : " + heuresDejaImputees
                            + "h déjà imputée(s), " + heures + "h supplémentaire(s) porteraient le total à "
                            + totalJournalier + "h (maximum 8h par jour).");
        }

        affectationRepository.findByCollaborateurIdAndProjetId(employe.getId(), projet.getId())
                .orElseThrow(() -> new ValidationException(
                        "Aucune affectation de " + employe.getNomComplet() + " au projet " + projet.getNom() + "."));

        imputationRepository.findDoublon(employe.getId(), projet.getId(), date, excludeImputationId)
                .ifPresent(existing -> {
                    throw new ValidationException(
                            "Une imputation existe déjà pour " + employe.getNomComplet() + " sur le projet "
                                    + projet.getNom() + " à la date du " + date + ".");
                });
    }

    private Imputation getOrThrow(Long id) {
        return imputationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Imputation introuvable : " + id));
    }

    private ImputationDTO toDTO(Imputation i) {
        return new ImputationDTO(
                i.getId(),
                i.getNom(),
                i.getDateImputation(),
                i.getHeures(),
                i.getStatut(),
                i.getProjet().getId(),
                i.getProjet().getNom(),
                i.getEmploye().getId(),
                i.getEmploye().getNomComplet(),
                i.getManagerValidateur() != null ? i.getManagerValidateur().getId() : null,
                i.getDateValidation(),
                i.getMotifRejet()
        );
    }
}
