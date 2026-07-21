package com.SSS.SGI.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "collaborateur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type_collaborateur", discriminatorType = DiscriminatorType.STRING)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"affectations"})
public class Collaborateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_collaborateur")
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank(message = "Le nom du collaborateur est obligatoire")
    @Column(name = "nom", nullable = false, length = 50)
    private String nom;

    @NotBlank(message = "Le prénom du collaborateur est obligatoire")
    @Column(name = "prenom", nullable = false, length = 50)
    private String prenom;

    @NotBlank(message = "L'email du collaborateur est obligatoire")
    @Email(message = "L'email doit être valide")
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @NotBlank(message = "Le mot de passe du collaborateur est obligatoire")
    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse;

    @JsonIgnore
    @OneToMany(mappedBy = "collaborateur", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Affectation> affectations = new HashSet<>();

    public String getNomComplet() {
        return prenom + " " + nom;
    }

    public void addAffectation(Affectation affectation) {
        affectations.add(affectation);
        affectation.setCollaborateur(this);
    }

    public void removeAffectation(Affectation affectation) {
        affectations.remove(affectation);
        affectation.setCollaborateur(null);
    }
}