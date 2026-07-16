package com.SSS.SGI.service;

import com.SSS.SGI.entity.Imputation;
import com.SSS.SGI.entity.Manager;
import com.SSS.SGI.entity.StatutImputation;
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

/**
 * Service pour gérer les imputations
 * Permet le CRUD sur les imputations pour les employés
 */
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

    /**
     * Crée une nouvelle imputation (employé)
     * L'employé crée une imputation pour être ensuite validée par un manager
     */
    public Imputation createImputation(Imputation imputation) {
        if (imputation.getEmploye() == null || imputation.getProjet() == null) {
            throw new IllegalArgumentException("L'imputation doit avoir un employé et un projet");
        }
        imputation.setStatut(StatutImputation.EN_ATTENTE);
        return imputationRepository.save(imputation);
    }

    /**
     * Récupère une imputation par son ID
     */
    public Optional<Imputation> getImputationById(Long id) {
        return imputationRepository.findById(id);
    }

    /**
     * Récupère toutes les imputations
     */
    public List<Imputation> getAllImputations() {
        return imputationRepository.findAll();
    }

    /**
     * Récupère les imputations d'un employé
     */
    public List<Imputation> getImputationsByEmploye(Long employeId) {
        return imputationRepository.findByEmployeId(employeId);
    }

    /**
     * Récupère les imputations d'un projet
     */
    public List<Imputation> getImputationsByProjet(Long projetId) {
        return imputationRepository.findByProjetId(projetId);
    }

    /**
     * Récupère les imputations d'un employé et d'un projet
     */
    public List<Imputation> getImputationsByEmployeIdAndProjetId(Long employeId, Long projetId) {
        return imputationRepository.findByEmployeIdAndProjetId(employeId, projetId);
    }

    /**
     * Récupère les imputations avec un statut spécifique
     */
    public List<Imputation> getImputationsByStatut(StatutImputation statut) {
        return imputationRepository.findByStatut(statut);
    }

    /**
     * Récupère les imputations validées par un manager
     */
    public List<Imputation> getImputationsByManager(Long managerId) {
        return imputationRepository.findByManagerValidateurId(managerId);
    }

    /**
     * Récupère les imputations d'un employé avec un statut spécifique
     */
    public List<Imputation> getImputationsByEmployeAndStatut(Long employeId, StatutImputation statut) {
        return imputationRepository.findByEmployeIdAndStatut(employeId, statut);
    }

    /**
     * Met à jour une imputation (employé)
     * L'employé peut modifier une imputation en attente
     */
    public Imputation updateImputation(Long id, Imputation updatedImputation) {
        Optional<Imputation> imputation = imputationRepository.findById(id);
        if (imputation.isPresent()) {
            Imputation i = imputation.get();
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
     * L'employé peut supprimer une imputation en attente
     */
    public void deleteImputation(Long id) {
        Optional<Imputation> imputation = imputationRepository.findById(id);
        if (imputation.isPresent()) {
            Imputation i = imputation.get();
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
     * Le manager peut valider une imputation en attente
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

        i.valider(m);
        return imputationRepository.save(i);
    }

    /**
     * Rejette une imputation (manager)
     * Le manager peut rejeter une imputation en attente
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

        i.rejeter(m);
        return imputationRepository.save(i);
    }

    /**
     * Récupère les imputations en attente de validation pour un manager
     */
    public List<Imputation> getImputationsEnAttenteForManager() {
        return imputationRepository.findByStatut(StatutImputation.EN_ATTENTE);
    }
}



