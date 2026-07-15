package com.SSS.SGI.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "collaborateur")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type_collaborateur", discriminatorType = DiscriminatorType.STRING)
public class Collaborateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_collaborateur")
    private Long id;

    @Column(name = "nom", nullable = false, length = 50)
    private String nom;

    @Column(name = "prenom", nullable = false, length = 50)
    private String prenom;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse;

    @OneToMany(mappedBy = "collaborateur", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Affectation> affectations = new HashSet<>();

    /**
     * Retourne le nom complet du collaborateur
     */
    public String getNomComplet() {
        return prenom + " " + nom;
    }

    /**
     * Ajoute une affectation
     */
    public void addAffectation(Affectation affectation) {
        affectations.add(affectation);
        affectation.setCollaborateur(this);
    }

    /**
     * Retire une affectation
     */
    public void removeAffectation(Affectation affectation) {
        affectations.remove(affectation);
        affectation.setCollaborateur(null);
    }
}

