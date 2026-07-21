package com.SSS.SGI.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "affectation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@IdClass(AffectationId.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"collaborateur", "projet"})
public class Affectation implements Serializable {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_collaborateur", nullable = false)
    @EqualsAndHashCode.Include
    private Collaborateur collaborateur;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_projet", nullable = false)
    @EqualsAndHashCode.Include
    private Projet projet;

    @Column(name = "taux_affectation", nullable = false, precision = 5, scale = 2)
    private BigDecimal tauxAffectation;

    @Column(name = "date_affectation")
    private LocalDate dateAffectation;
}