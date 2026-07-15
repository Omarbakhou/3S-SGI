package com.SSS.SGI.service;

import com.SSS.SGI.entity.Collaborateur;
import com.SSS.SGI.entity.Employe;
import com.SSS.SGI.entity.Manager;
import com.SSS.SGI.repository.CollaborateurRepository;
import com.SSS.SGI.repository.EmployeRepository;
import com.SSS.SGI.repository.ManagerRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private CollaborateurRepository collaborateurRepository;

    @Autowired
    private EmployeRepository employeRepository;

    @Autowired
    private ManagerRepository managerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
     * Récupère tous les collaborateurs
     */
    public List<Collaborateur> getAllCollaborateurs() {
        return collaborateurRepository.findAll();
    }

    /**
     * Crée un nouvel employé
     */
    public Employe createEmploye(Employe employe) {
        employe.setMotDePasse(passwordEncoder.encode(employe.getMotDePasse()));
        return employeRepository.save(employe);
    }

    /**
     * Crée un nouveau manager
     */
    public Manager createManager(Manager manager) {
        manager.setMotDePasse(passwordEncoder.encode(manager.getMotDePasse()));
        return managerRepository.save(manager);
    }

    /**
     * Met à jour le profil du collaborateur (nom, prénom, email)
     * Permet au collaborateur de modifier ses données
     */
    public Collaborateur updateCollaborateurProfile(Long id, String nom, String prenom, String email) {
        Optional<Collaborateur> collaborateur = collaborateurRepository.findById(id);
        if (collaborateur.isPresent()) {
            Collaborateur c = collaborateur.get();
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
     * Récupère un manager par son ID
     */
    public Optional<Manager> getManagerById(Long id) {
        return managerRepository.findById(id);
    }
}

