package com.SSS.SGI.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la mise à jour du profil
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    private String nom;
    private String prenom;
    private String email;
    private String motDePasseActuel;
}

