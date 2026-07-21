package com.SSS.SGI.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "projet")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type_projet", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("PROJET")
@EqualsAndHashCode(exclude = {"client", "imputations", "affectations"})
@ToString(exclude = {"client", "imputations", "affectations"})
public class Projet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_projet")
    private Long id;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_client", nullable = false)
    private Client client;


    @JsonIgnore
    @OneToMany(mappedBy = "projet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Imputation> imputations = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "projet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Affectation> affectations = new HashSet<>();

    /**
     * Ajoute une imputation
     */
    public void addImputation(Imputation imputation) {
        imputations.add(imputation);
        imputation.setProjet(this);
    }

    /**
     * Retire une imputation
     */
    public void removeImputation(Imputation imputation) {
        imputations.remove(imputation);
        imputation.setProjet(null);
    }

    /**
     * Ajoute une affectation
     */
    public void addAffectation(Affectation affectation) {
        affectations.add(affectation);
        affectation.setProjet(this);
    }

    /**
     * Retire une affectation
     */
    public void removeAffectation(Affectation affectation) {
        affectations.remove(affectation);
        affectation.setProjet(null);
    }
}

