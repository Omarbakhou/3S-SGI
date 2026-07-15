package com.SSS.SGI.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour mettre à jour un projet avec budget
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBudgetProjetRequest {
    private BigDecimal budgetInitial;
    private BigDecimal tjm;
}

