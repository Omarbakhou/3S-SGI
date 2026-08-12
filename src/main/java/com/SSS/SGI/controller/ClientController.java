package com.SSS.SGI.controller;

import com.SSS.SGI.entity.Client;
import com.SSS.SGI.service.ProjetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Clients", description = "Gestion des clients (donneurs d'ordre des projets)")
public class ClientController {

    private final ProjetService projetService;

    public ClientController(ProjetService projetService) {
        this.projetService = projetService;
    }

    @Operation(summary = "Créer un client", description = "Accessible uniquement par les managers.")
    @ApiResponse(responseCode = "201", description = "Client créé")
    @ApiResponse(responseCode = "400", description = "Nom du client manquant")
    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Client> createClient(@Valid @RequestBody Client client) {
        Client created = projetService.createClient(client);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @Operation(summary = "Récupérer un client par ID", description = "Accessible par les managers.")
    @ApiResponse(responseCode = "200", description = "Client trouvé")
    @ApiResponse(responseCode = "404", description = "Client introuvable")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<Client> getClient(@PathVariable Long id) {
        Optional<Client> client = projetService.getClientById(id);
        return client.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Récupérer un client par son nom", description = "Accessible par les managers.")
    @ApiResponse(responseCode = "200", description = "Client trouvé")
    @ApiResponse(responseCode = "404", description = "Client introuvable")
    @GetMapping("/nom/{nom}")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<Client> findByNomClient(@PathVariable String nom) {
        Optional<Client> client = projetService.findByNomClient(nom);
        return client.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Lister tous les clients", description = "Accessible par les managers.")
    @ApiResponse(responseCode = "200", description = "Liste des clients")
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<List<Client>> getAllClients() {
        List<Client> clients = projetService.getAllClients();
        return ResponseEntity.ok(clients);
    }

    @Operation(summary = "Mettre à jour un client", description = "Accessible uniquement par les managers.")
    @ApiResponse(responseCode = "200", description = "Client mis à jour")
    @ApiResponse(responseCode = "400", description = "Client introuvable")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Client> updateClient(
            @PathVariable Long id,
            @Valid @RequestBody Client updatedClient) {
        Client updated = projetService.updateClient(id, updatedClient);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Supprimer un client", description = "Accessible uniquement par les managers.")
    @ApiResponse(responseCode = "200", description = "Client supprimé")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<String> deleteClient(@PathVariable Long id) {
        projetService.deleteClient(id);
        return ResponseEntity.ok("Client supprimé avec succès");
    }
}