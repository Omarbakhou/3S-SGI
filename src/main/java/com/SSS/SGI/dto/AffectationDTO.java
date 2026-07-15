package com.SSS.SGI.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AffectationDTO {

    private Long collaborateurId;
    private String nomCollaborateur;
    private Long projetId;
    private String nomProjet;
    private BigDecimal tauxAffectation;
    private LocalDate dateAffectation;
}

