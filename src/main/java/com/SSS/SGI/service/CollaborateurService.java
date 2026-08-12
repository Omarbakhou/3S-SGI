package com.SSS.SGI.service;

import com.SSS.SGI.entity.Collaborateur;
import com.SSS.SGI.entity.Employe;
import com.SSS.SGI.entity.Manager;
import com.SSS.SGI.repository.CollaborateurRepository;
import com.SSS.SGI.repository.EmployeRepository;
import com.SSS.SGI.repository.ManagerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service pour gérer les collaborateurs
 * Permet le CRUD sur le compte collaborateur avec les méthodes appropriées
 */
@Service
@Transactional
public class CollaborateurService {

    private final CollaborateurRepository collaborateurRepository;

    private final EmployeRepository employeRepository;

    private final ManagerRepository managerRepository;

    private final PasswordEncoder passwordEncoder;

    public CollaborateurService(CollaborateurRepository collaborateurRepository, EmployeRepository employeRepository, ManagerRepository managerRepository, PasswordEncoder passwordEncoder) {
        this.collaborateurRepository = collaborateurRepository;
        this.employeRepository = employeRepository;
        this.managerRepository = managerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Récupère un collaborateur par son ID
     */
    public Optional<Collaborateur> getCollaborateurById(Long id) {
        return collaborateurRepository.findById(id);
    }

    /**
     * Récupère un collaborateur par son email
     */
    public Optional<Collaborateur> getCollaborateurByEmail(String email) {
        return collaborateurRepository.findByEmail(email);
    }

    /**
     * Récupère un collaborateur par son nom et prénom
     */
    public Optional<Collaborateur> getCollaborateurByNomAndPrenom(String nom, String prenom) {
        return collaborateurRepository.findByNomAndPrenom(nom, prenom);
    }
    /**
     * Récupère tous les collaborateurs
     */
    public List<Collaborateur> getAllCollaborateurs() {
        return collaborateurRepository.findAll();
    }

    /**
     * Crée un nouvel employé
     */
    public Employe createEmploye(Employe employe) {
        // Validation des champs obligatoires
        if (employe.getNom() == null || employe.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de l'employé est obligatoire");
        }
        if (employe.getPrenom() == null || employe.getPrenom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le prénom de l'employé est obligatoire");
        }
        if (employe.getEmail() == null || employe.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("L'email de l'employé est obligatoire");
        }
        if (employe.getMotDePasse() == null || employe.getMotDePasse().trim().isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe de l'employé est obligatoire");
        }

        employe.setMotDePasse(passwordEncoder.encode(employe.getMotDePasse()));
        return employeRepository.save(employe);
    }

    /**
     * Crée un nouveau manager
     */
    public Manager createManager(Manager manager) {
        // Validation des champs obligatoires
        if (manager.getNom() == null || manager.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du manager est obligatoire");
        }
        if (manager.getPrenom() == null || manager.getPrenom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le prénom du manager est obligatoire");
        }
        if (manager.getEmail() == null || manager.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("L'email du manager est obligatoire");
        }
        if (manager.getMotDePasse() == null || manager.getMotDePasse().trim().isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe du manager est obligatoire");
        }

        manager.setMotDePasse(passwordEncoder.encode(manager.getMotDePasse()));
        return managerRepository.save(manager);
    }

    /**
     * Met à jour le profil du collaborateur (nom, prénom, email)
     * Permet au collaborateur de modifier ses données
     */
    public Collaborateur updateCollaborateurProfile(Long id, String nom, String prenom, String email, String motDePasseActuel) {
        Optional<Collaborateur> collaborateur = collaborateurRepository.findById(id);
        if (collaborateur.isPresent()) {
            Collaborateur c = collaborateur.get();
            // Pas de session/JWT pour identifier l'appelant : le mot de passe actuel
            // sert de preuve de propriété du compte, comme pour changePassword.
            if (motDePasseActuel == null || !passwordEncoder.matches(motDePasseActuel, c.getMotDePasse())) {
                throw new IllegalArgumentException("Le mot de passe actuel est incorrect");
            }
            c.setNom(nom);
            c.setPrenom(prenom);
            c.setEmail(email);
            return collaborateurRepository.save(c);
        }
        throw new IllegalArgumentException("Collaborateur non trouvé avec l'ID: " + id);
    }

    /**
     * Change le mot de passe du collaborateur
     * Permet au collaborateur de modifier son mot de passe
     */
    public void changePassword(Long id, String ancienMotDePasse, String nouveauMotDePasse) {
        Optional<Collaborateur> collaborateur = collaborateurRepository.findById(id);
        if (collaborateur.isPresent()) {
            Collaborateur c = collaborateur.get();
            if (!passwordEncoder.matches(ancienMotDePasse, c.getMotDePasse())) {
                throw new IllegalArgumentException("L'ancien mot de passe est incorrect");
            }
            c.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
            collaborateurRepository.save(c);
        } else {
            throw new IllegalArgumentException("Collaborateur non trouvé avec l'ID: " + id);
        }
    }

    /**
     * Supprime un collaborateur
     */
    public void deleteCollaborateur(Long id) {
        collaborateurRepository.deleteById(id);
    }

    /**
     * Récupère tous les employés
     */
    public List<Employe> getAllEmployes() {
        return employeRepository.findAll();
    }

    /**
     * Récupère tous les managers
     */
    public List<Manager> getAllManagers() {
        return managerRepository.findAll();
    }

    /**
     * Récupère un employé par son ID
     */
    public Optional<Employe> getEmployeById(Long id) {
        return employeRepository.findById(id);
    }

    /**
     * Récupère un employé par son email
     */
    public Optional<Employe> findEmployeByEmail(String email) {
        return employeRepository.findEmployeByEmail(email);
    }

    /**
     * Récupère un manager par son ID
     */
    public Optional<Manager> getManagerById(Long id) {
        return managerRepository.findById(id);
    }

    /**
     * Récupère un manager par son email
     */
    public Optional<Manager> findByEmail(String email) {
        return managerRepository.findByEmail(email);
    }

}

