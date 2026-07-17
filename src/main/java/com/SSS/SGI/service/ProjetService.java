package com.SSS.SGI.service;

import com.SSS.SGI.entity.Client;
import com.SSS.SGI.entity.Projet;
import com.SSS.SGI.entity.BudgetProjet;
import com.SSS.SGI.repository.ClientRepository;
import com.SSS.SGI.repository.ProjetRepository;
import com.SSS.SGI.repository.BudgetProjetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service pour gérer les clients et projets
 */
@Service
@Transactional
public class ProjetService {

    private final ProjetRepository projetRepository;

    private final ClientRepository clientRepository;

    private final BudgetProjetRepository budgetProjetRepository;

    public ProjetService(ProjetRepository projetRepository, ClientRepository clientRepository, BudgetProjetRepository budgetProjetRepository) {
        this.projetRepository = projetRepository;
        this.clientRepository = clientRepository;
        this.budgetProjetRepository = budgetProjetRepository;
    }

    /**
     * Crée un nouveau client
     */
    public Client createClient(Client client) {
        // Validation des champs obligatoires
        if (client.getNomClient() == null || client.getNomClient().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du client est obligatoire");
        }
        return clientRepository.save(client);
    }

    /**
     * Récupère un client par son ID
     */
    public Optional<Client> getClientById(Long id) {
        return clientRepository.findById(id);
    }

    /**
     * Récupère tous les clients
     */
    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    /**
     * Récupère un client par son nom
     */
    public Optional<Client> findByNomClient(String nomClient) {
        return clientRepository.findByNomClient(nomClient);
    }

    /**
     * Met à jour un client
     */
    public Client updateClient(Long id, Client updatedClient) {
        Optional<Client> client = clientRepository.findById(id);
        if (client.isPresent()) {
            Client c = client.get();
            c.setNomClient(updatedClient.getNomClient());
            return clientRepository.save(c);
        }
        throw new IllegalArgumentException("Client non trouvé avec l'ID: " + id);
    }

    /**
     * Supprime un client
     */
    public void deleteClient(Long id) {
        clientRepository.deleteById(id);
    }

    /**
     * Crée un nouveau projet
     */
    public Projet createProjet(Projet projet) {
        if (projet.getClient() == null) {
            throw new IllegalArgumentException("Le projet doit avoir un client");
        }
        return projetRepository.save(projet);
    }

    /**
     * Récupère un projet par son ID
     */
    public Optional<Projet> getProjetById(Long id) {
        return projetRepository.findById(id);
    }

    /**
     * Récupère un projet par son nom
     */
    public Optional<Projet> findProjetByNom(String nom) {
        return projetRepository.findProjetByNom(nom);
    }

    /**
     * Récupère tous les projets
     */
    public List<Projet> getAllProjets() {
        return projetRepository.findAll();
    }

    /**
     * Récupère les projets d'un client
     */
    public List<Projet> getProjetsByClient(Long clientId) {
        return projetRepository.findProjetByClientId(clientId);
    }

    /**
     * Met à jour un projet
     */
    public Projet updateProjet(Long id, Projet updatedProjet) {
        Optional<Projet> projet = projetRepository.findById(id);
        if (projet.isPresent()) {
            Projet p = projet.get();
            p.setNom(updatedProjet.getNom());
            p.setDateDebut(updatedProjet.getDateDebut());
            p.setDateFin(updatedProjet.getDateFin());
            return projetRepository.save(p);
        }
        throw new IllegalArgumentException("Projet non trouvé avec l'ID: " + id);
    }

    /**
     * Supprime un projet
     */
    public void deleteProjet(Long id) {
        projetRepository.deleteById(id);
    }

    /**
     * Crée un nouveau projet avec budget
     */
    public BudgetProjet createBudgetProjet(BudgetProjet budgetProjet) {
        if (budgetProjet.getClient() == null) {
            throw new IllegalArgumentException("Le projet budget doit avoir un client");
        }
        if (budgetProjet.getBudgetInitial() == null || budgetProjet.getTjm() == null) {
            throw new IllegalArgumentException("Le budget initial et le TJM sont obligatoires");
        }
        return budgetProjetRepository.save(budgetProjet);
    }

    /**
     * Récupère un projet avec budget par son ID
     */
    public Optional<BudgetProjet> getBudgetProjetById(Long id) {
        return budgetProjetRepository.findById(id);
    }

    /**
     * Récupère tous les projets avec budget
     */
    public List<BudgetProjet> getAllBudgetProjets() {
        return budgetProjetRepository.findAll();
    }

    /**
     * Récupère les projets avec budget d'un client
     */
    public List<BudgetProjet> getBudgetProjetsByClient(Long clientId) {
        return budgetProjetRepository.findByClientId(clientId);
    }

    /**
     * Met à jour un projet avec budget
     */
    public BudgetProjet updateBudgetProjet(Long id, BigDecimal budgetInitial, BigDecimal tjm) {
        Optional<BudgetProjet> budgetProjet = budgetProjetRepository.findById(id);
        if (budgetProjet.isPresent()) {
            BudgetProjet bp = budgetProjet.get();
            bp.setBudgetInitial(budgetInitial);
            bp.setTjm(tjm);
            return budgetProjetRepository.save(bp);
        }
        throw new IllegalArgumentException("Projet avec budget non trouvé avec l'ID: " + id);
    }

    /**
     * Supprime un projet avec budget
     */
    public void deleteBudgetProjet(Long id) {
        budgetProjetRepository.deleteById(id);
    }
}

