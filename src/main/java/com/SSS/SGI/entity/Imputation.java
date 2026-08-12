package com.SSS.SGI.entity;

import com.SSS.SGI.interfaces.ImputationInterface;
import com.SSS.SGI.interfaces.ValidationInterface;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "imputation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"projet", "employe", "managerValidateur"})
public class Imputation implements ImputationInterface, ValidationInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_imputation")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;


    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutImputation statut = StatutImputation.EN_ATTENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_projet", nullable = false)
    private Projet projet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_employe", nullable = false)
    private Employe employe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_manager_validateur")
    private Manager managerValidateur;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    // ========== Implémentation de l'interface Imputation ==========

    @Override
    public String getNomImputation() {
        return this.nom;
    }

    @Override
    public void setNomImputation(String nom) {
        this.nom = nom;
    }

    /**
     * Retourne le nom du projet associé à l'imputation
     */
    @Override
    public String getNomProjet() {
        return projet != null ? projet.getNom() : "";
    }

    /**
     * Retourne le nom complet de l'employé qui a saisi l'imputation
     */
    @Override
    public String getNomEmploye() {
        return employe != null ? employe.getNomComplet() : "";
    }

    @Override
    public StatutImputation getStatutImputation() {
        return statut;
    }

    @Override
    public void setStatutImputation(StatutImputation statut) {
        this.statut = statut;
    }

    /**
     * Retourne le label du statut
     */
    public String getStatutLabel() {
        return statut != null ? statut.getLabel() : "";
    }

    // ========== Implémentation de l'interface ValidationInterface ==========

    /**
     * Valide cette imputation
     *
     * @param manager le manager qui valide
     * @throws IllegalStateException si l'imputation n'est pas en attente
     */
    @Override
    public void valider(Manager manager) {
        if (this.statut != StatutImputation.EN_ATTENTE) {
            throw new IllegalStateException("Seule une imputation en attente peut être validée");
        }
        this.statut = StatutImputation.VALIDEE;
        this.managerValidateur = manager;
        this.dateValidation = LocalDateTime.now();
    }

    /**
     * Rejette cette imputation
     *
     * @param manager le manager qui rejette
     * @throws IllegalStateException si l'imputation n'est pas en attente
     */
    @Override
    public void rejeter(Manager manager) {
        if (this.statut != StatutImputation.EN_ATTENTE) {
            throw new IllegalStateException("Seule une imputation en attente peut être rejetée");
        }
        this.statut = StatutImputation.REJETEE;
        this.managerValidateur = manager;
        this.dateValidation = LocalDateTime.now();
    }

    @Override
    public Manager getManagerValidateur() {
        return this.managerValidateur;
    }

    @Override
    public LocalDateTime getDateValidation() {
        return this.dateValidation;
    }

    /**
     * Vérifie si l'imputation peut être validée/rejetée
     */
    public boolean peutEtreValidee() {
        return this.statut == StatutImputation.EN_ATTENTE;
    }
}

