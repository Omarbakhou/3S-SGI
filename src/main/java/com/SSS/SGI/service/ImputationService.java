package com.SSS.SGI.service;

import com.SSS.SGI.entity.Imputation;
import com.SSS.SGI.entity.Manager;
import com.SSS.SGI.entity.StatutImputation;
import com.SSS.SGI.exception.ImputationNonAutoriseeException;
import com.SSS.SGI.exception.ManagerNonAutoriseException;
import com.SSS.SGI.repository.ImputationRepository;
import com.SSS.SGI.repository.EmployeRepository;
import com.SSS.SGI.repository.ManagerRepository;
import com.SSS.SGI.repository.ProjetRepository;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ImputationService {

    private final ImputationRepository imputationRepository;

    @Setter
    @Getter
    private EmployeRepository employeRepository;

    private final ManagerRepository managerRepository;

    @Setter
    @Getter
    private ProjetRepository projetRepository;

    public ImputationService(ImputationRepository imputationRepository, EmployeRepository employeRepository, ManagerRepository managerRepository, ProjetRepository projetRepository) {
        this.imputationRepository = imputationRepository;
        this.employeRepository = employeRepository;
        this.managerRepository = managerRepository;
        this.projetRepository = projetRepository;
    }

    public Imputation createImputation(Imputation imputation) {
        if (imputation.getEmploye() == null || imputation.getProjet() == null) {
            throw new IllegalArgumentException("L'imputation doit avoir un employé et un projet");
        }
        imputation.setStatut(StatutImputation.EN_ATTENTE);
        return imputationRepository.save(imputation);
    }

    public Optional<Imputation> getImputationById(Long id) {
        return imputationRepository.findById(id);
    }

    public List<Imputation> getAllImputations() {
        return imputationRepository.findAll();
    }

    public List<Imputation> getImputationsByEmploye(Long employeId) {
        return imputationRepository.findByEmployeId(employeId);
    }

    public List<Imputation> getImputationsByProjet(Long projetId) {
        return imputationRepository.findByProjetId(projetId);
    }

    public List<Imputation> getImputationsByEmployeIdAndProjetId(Long employeId, Long projetId) {
        return imputationRepository.findByEmployeIdAndProjetId(employeId, projetId);
    }

    public List<Imputation> getImputationsByStatut(StatutImputation statut) {
        return imputationRepository.findByStatut(statut);
    }

    public List<Imputation> getImputationsByManager(Long managerId) {
        return imputationRepository.findByManagerValidateurId(managerId);
    }

    public List<Imputation> getImputationsByEmployeAndStatut(Long employeId, StatutImputation statut) {
        return imputationRepository.findByEmployeIdAndStatut(employeId, statut);
    }

    /**
     * Met à jour une imputation (employé)
     * L'employé ne peut modifier que ses propres imputations, en attente uniquement
     */
    public Imputation updateImputation(Long id, Long employeId, Imputation updatedImputation) {
        Optional<Imputation> imputation = imputationRepository.findById(id);
        if (imputation.isPresent()) {
            Imputation i = imputation.get();
            if (!i.getEmploye().getId().equals(employeId)) {
                throw new ImputationNonAutoriseeException(
                        "Cette imputation n'appartient pas à cet employé.");
            }
            if (i.getStatut() != StatutImputation.EN_ATTENTE) {
                throw new IllegalStateException("Seule une imputation en attente peut être modifiée");
            }
            i.setNom(updatedImputation.getNom());
            if (updatedImputation.getProjet() != null) {
                i.setProjet(updatedImputation.getProjet());
            }
            return imputationRepository.save(i);
        }
        throw new IllegalArgumentException("Imputation non trouvée avec l'ID: " + id);
    }

    /**
     * Supprime une imputation (employé)
     * L'employé ne peut supprimer que ses propres imputations, en attente uniquement
     */
    public void deleteImputation(Long id, Long employeId) {
        Optional<Imputation> imputation = imputationRepository.findById(id);
        if (imputation.isPresent()) {
            Imputation i = imputation.get();
            if (!i.getEmploye().getId().equals(employeId)) {
                throw new ImputationNonAutoriseeException(
                        "Cette imputation n'appartient pas à cet employé.");
            }
            if (i.getStatut() != StatutImputation.EN_ATTENTE) {
                throw new IllegalStateException("Seule une imputation en attente peut être supprimée");
            }
            imputationRepository.deleteById(id);
        } else {
            throw new IllegalArgumentException("Imputation non trouvée avec l'ID: " + id);
        }
    }

    /**
     * Valide une imputation (manager)
     * Le manager doit être celui de l'employé concerné
     */
    public Imputation validerImputation(Long imputationId, Long managerId) {
        Optional<Imputation> imputation = imputationRepository.findById(imputationId);
        Optional<Manager> manager = managerRepository.findById(managerId);

        if (imputation.isEmpty()) {
            throw new IllegalArgumentException("Imputation non trouvée avec l'ID: " + imputationId);
        }
        if (manager.isEmpty()) {
            throw new IllegalArgumentException("Manager non trouvé avec l'ID: " + managerId);
        }

        Imputation i = imputation.get();
        Manager m = manager.get();

        verifierLegitimiteManager(i, managerId);

        i.valider(m);
        return imputationRepository.save(i);
    }

    /**
     * Rejette une imputation (manager)
     * Le manager doit être celui de l'employé concerné
     */
    public Imputation rejeterImputation(Long imputationId, Long managerId) {
        Optional<Imputation> imputation = imputationRepository.findById(imputationId);
        Optional<Manager> manager = managerRepository.findById(managerId);

        if (imputation.isEmpty()) {
            throw new IllegalArgumentException("Imputation non trouvée avec l'ID: " + imputationId);
        }
        if (manager.isEmpty()) {
            throw new IllegalArgumentException("Manager non trouvé avec l'ID: " + managerId);
        }

        Imputation i = imputation.get();
        Manager m = manager.get();

        verifierLegitimiteManager(i, managerId);

        i.rejeter(m);
        return imputationRepository.save(i);
    }

    /**
     * Récupère les imputations en attente de validation pour un manager précis
     * (uniquement celles de ses propres employés)
     */
    public List<Imputation> getImputationsEnAttenteForManager(Long managerId) {
        return imputationRepository.findByStatut(StatutImputation.EN_ATTENTE).stream()
                .filter(i -> i.getEmploye().getManager() != null
                        && i.getEmploye().getManager().getId().equals(managerId))
                .toList();
    }

    private void verifierLegitimiteManager(Imputation imputation, Long managerId) {
        if (imputation.getEmploye().getManager() == null
                || !imputation.getEmploye().getManager().getId().equals(managerId)) {
            throw new ManagerNonAutoriseException(
                    "Ce manager n'est pas autorisé à valider les imputations de cet employé.");
        }
    }
}