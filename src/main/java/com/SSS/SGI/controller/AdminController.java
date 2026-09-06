package com.SSS.SGI.controller;

import com.SSS.SGI.dto.CompteDTO;
import com.SSS.SGI.security.CustomUserDetails;
import com.SSS.SGI.service.AdminCompteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Actions réservées aux administrateurs sur les comptes.
 *
 * <p>L'identifiant dans l'URL désigne le compte <em>cible</em> ; l'identité de
 * l'administrateur qui agit vient exclusivement du jeton ({@code principal.getId()}),
 * jamais d'un paramètre client. C'est ce qui permet de refuser de façon fiable
 * l'auto-désactivation.
 *
 * <p>Aucune suppression physique n'est exposée : retirer un accès se fait par
 * désactivation, pour préserver l'historique des imputations et des absences.
 */
@RestController
@RequestMapping("/api/admin/comptes")
@Tag(name = "Administration des comptes", description = "Activation et désactivation des comptes (ADMIN uniquement)")
public class AdminController {

    private final AdminCompteService adminCompteService;

    public AdminController(AdminCompteService adminCompteService) {
        this.adminCompteService = adminCompteService;
    }

    @Operation(
            summary = "Désactiver un compte",
            description = "Bascule le compte à inactif sans rien supprimer. Refuse l'auto-désactivation "
                    + "et la désactivation du dernier administrateur actif.")
    @ApiResponse(responseCode = "200", description = "Compte désactivé")
    @ApiResponse(responseCode = "403", description = "Rôle ADMIN requis")
    @ApiResponse(responseCode = "404", description = "Compte introuvable")
    @ApiResponse(responseCode = "409", description = "Désactivation refusée (propre compte ou dernier administrateur)")
    @PatchMapping("/{id:\\d+}/desactiver")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompteDTO> desactiver(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(CompteDTO.from(adminCompteService.desactiver(id, principal.getId())));
    }

    @Operation(summary = "Réactiver un compte", description = "Rend l'accès à un compte précédemment désactivé.")
    @ApiResponse(responseCode = "200", description = "Compte réactivé")
    @ApiResponse(responseCode = "403", description = "Rôle ADMIN requis")
    @ApiResponse(responseCode = "404", description = "Compte introuvable")
    @PatchMapping("/{id:\\d+}/reactiver")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompteDTO> reactiver(@PathVariable Long id) {
        return ResponseEntity.ok(CompteDTO.from(adminCompteService.reactiver(id)));
    }
}
