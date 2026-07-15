package com.SSS.SGI.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "budget_projet")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@DiscriminatorValue("BUDGET")
public class BudgetProjet extends Projet {

    @Column(name = "budget_initial", nullable = false, precision = 12, scale = 2)
    private BigDecimal budgetInitial;

    @Column(name = "tjm", nullable = false, precision = 8, scale = 2)
    private BigDecimal tjm; // Tarif Journalier Moyen

}

