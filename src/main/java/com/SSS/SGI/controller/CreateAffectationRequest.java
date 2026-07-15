package com.SSS.SGI.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO pour créer une affectation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAffectationRequest {
    private Long collaborateurId;
    private Long projetId;
    private BigDecimal tauxAffectation;
    private LocalDate dateAffectation;
}

