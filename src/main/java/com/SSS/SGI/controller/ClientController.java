package com.SSS.SGI.controller;

import com.SSS.SGI.entity.Client;
import com.SSS.SGI.service.ProjetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/clients")
@CrossOrigin(origins = "*")
public class ClientController {

    private final ProjetService projetService;

    public ClientController(ProjetService projetService) {
        this.projetService = projetService;
    }

    /**
     * Crée un nouveau client
     * Accessible uniquement par les managers
     */
    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Client> createClient(@Valid @RequestBody Client client) {
        Client created = projetService.createClient(client);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Récupère un client par son ID
     * Accessible par les managers
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<Client> getClient(@PathVariable Long id) {
        Optional<Client> client = projetService.getClientById(id);
        return client.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Récupère un client par son nom
     * Accessible par les managers
     */
    @GetMapping("/nom/{nom}")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<Client> findByNomClient(@PathVariable String nom) {
        Optional<Client> client = projetService.findByNomClient(nom);
        return client.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Récupère tous les clients
     * Accessible par les managers
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<List<Client>> getAllClients() {
        List<Client> clients = projetService.getAllClients();
        return ResponseEntity.ok(clients);
    }

    /**
     * Met à jour un client
     * Accessible uniquement par les managers
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Client> updateClient(
            @PathVariable Long id,
            @Valid @RequestBody Client updatedClient) {
        Client updated = projetService.updateClient(id, updatedClient);
        return ResponseEntity.ok(updated);
    }

    /**
     * Supprime un client
     * Accessible uniquement par les managers
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<String> deleteClient(@PathVariable Long id) {
        projetService.deleteClient(id);
        return ResponseEntity.ok("Client supprimé avec succès");
    }
}

