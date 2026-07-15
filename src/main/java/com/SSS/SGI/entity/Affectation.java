package com.SSS.SGI.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "affectation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(AffectationId.class)
public class Affectation implements Serializable {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_collaborateur", nullable = false)
    private Collaborateur collaborateur;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_projet", nullable = false)
    private Projet projet;

    @Column(name = "taux_affectation", nullable = false, precision = 5, scale = 2)
    private BigDecimal tauxAffectation;

    @Column(name = "date_affectation")
    private LocalDate dateAffectation;
}
