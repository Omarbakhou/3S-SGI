package com.SSS.SGI.dto;

import com.SSS.SGI.entity.Collaborateur;
import com.SSS.SGI.entity.Manager;

/**
 * Vue d'un compte renvoyée par les actions d'administration.
 * Volontairement réduite : ni mot de passe, ni collections liées.
 */
public record CompteDTO(
        Long id,
        String nom,
        String prenom,
        String nomComplet,
        String email,
        String type,
        boolean actif) {

    public static CompteDTO from(Collaborateur collaborateur) {
        return new CompteDTO(
                collaborateur.getId(),
                collaborateur.getNom(),
                collaborateur.getPrenom(),
                collaborateur.getNomComplet(),
                collaborateur.getEmail(),
                collaborateur instanceof Manager ? "Manager" : "Employé",
                collaborateur.isActif());
    }
}
