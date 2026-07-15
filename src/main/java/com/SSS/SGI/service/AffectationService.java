package com.SSS.SGI.service;

import com.SSS.SGI.entity.Affectation;
import com.SSS.SGI.entity.Collaborateur;
import com.SSS.SGI.entity.Projet;
import com.SSS.SGI.entity.AffectationId;
import com.SSS.SGI.repository.AffectationRepository;
import com.SSS.SGI.repository.CollaborateurRepository;
import com.SSS.SGI.repository.ProjetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service pour gérer les affectations
 * Gère la relation many-to-many entre Collaborateur et Projet
 * avec le taux d'affectation (décision métier)
 */
@Service
@Transactional
public class AffectationService {

    private final AffectationRepository affectationRepository;

    private final CollaborateurRepository collaborateurRepository;

    private final ProjetRepository projetRepository;

    public AffectationService(AffectationRepository affectationRepository, CollaborateurRepository collaborateurRepository, ProjetRepository projetRepository) {
        this.affectationRepository = affectationRepository;
        this.collaborateurRepository = collaborateurRepository;
        this.projetRepository = projetRepository;
    }

    public Affectation createAffectation(Long collaborateurId, Long projetId, BigDecimal tauxAffectation, LocalDate dateAffectation) {
        Optional<Collaborateur> collaborateur = collaborateurRepository.findById(collaborateurId);
        Optional<Projet> projet = projetRepository.findById(projetId);

        if (collaborateur.isEmpty()) {
            throw new IllegalArgumentException("Collaborateur non trouvé avec l'ID: " + collaborateurId);
        }
        if (projet.isEmpty()) {
            throw new IllegalArgumentException("Projet non trouvé avec l'ID: " + projetId);
        }

        // Validation du taux d'affectation
        if (tauxAffectation == null ||
            tauxAffectation.compareTo(BigDecimal.ZERO) < 0 ||
            tauxAffectation.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Le taux d'affectation doit être entre 0 et 100");
        }

        Affectation affectation = new Affectation();
        affectation.setCollaborateur(collaborateur.get());
        affectation.setProjet(projet.get());
        affectation.setTauxAffectation(tauxAffectation);
        affectation.setDateAffectation(dateAffectation);

        return affectationRepository.save(affectation);
    }

    /**
     * Récupère une affectation
     */
    public Optional<Affectation> getAffectation(Long collaborateurId, Long projetId) {
        return affectationRepository.findByCollaborateurIdAndProjetId(collaborateurId, projetId);
    }

    /**
     * Récupère toutes les affectations d'un collaborateur
     */
    public List<Affectation> getAffectationsByCollaborateur(Long collaborateurId) {
        return affectationRepository.findByCollaborateurId(collaborateurId);
    }

    /**
     * Récupère toutes les affectations d'un projet
     */
    public List<Affectation> getAffectationsByProjet(Long projetId) {
        return affectationRepository.findByProjetId(projetId);
    }

    /**
     * Récupère toutes les affectations
     */
    public List<Affectation> getAllAffectations() {
        return affectationRepository.findAll();
    }

    /**
     * Met à jour le taux d'affectation (décision métier du manager)
     */
    public Affectation updateTauxAffectation(Long collaborateurId, Long projetId, BigDecimal nouveauTaux) {
        Optional<Affectation> affectation = affectationRepository.findByCollaborateurIdAndProjetId(collaborateurId, projetId);

        if (affectation.isEmpty()) {
            throw new IllegalArgumentException("Affectation non trouvée");
        }

        // Validation du taux
        if (nouveauTaux == null ||
            nouveauTaux.compareTo(BigDecimal.ZERO) < 0 ||
            nouveauTaux.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Le taux d'affectation doit être entre 0 et 100");
        }

        Affectation a = affectation.get();
        a.setTauxAffectation(nouveauTaux);
        return affectationRepository.save(a);
    }

    /**
     * Supprime une affectation
     */
    public void deleteAffectation(Long collaborateurId, Long projetId) {
        AffectationId id = new AffectationId(collaborateurId, projetId);
        affectationRepository.deleteById(id);
    }

    /**
     * Calcule le taux d'affectation total d'un collaborateur
     * La somme de tous les taux d'affectation ne devrait pas dépasser 100%
     */
    public BigDecimal getTauxAffectationTotal(Long collaborateurId) {
        List<Affectation> affectations = getAffectationsByCollaborateur(collaborateurId);
        return affectations.stream()
            .map(Affectation::getTauxAffectation)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Vérifie si un collaborateur peut être affecté à un nouveau projet
     * (vérification du taux total)
     */
    public boolean canAffectCollaborateur(Long collaborateurId, BigDecimal nouveauTaux) {
        BigDecimal tauxTotal = getTauxAffectationTotal(collaborateurId);
        BigDecimal tauxFinal = tauxTotal.add(nouveauTaux);
        return tauxFinal.compareTo(new BigDecimal("100")) <= 0;
    }
}

