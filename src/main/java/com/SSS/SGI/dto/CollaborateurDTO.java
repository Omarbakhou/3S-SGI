package com.SSS.SGI.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollaborateurDTO {

    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String type; // "EMPLOYE" ou "MANAGER"

    public String getNomComplet() {
        return prenom + " " + nom;
    }

}

