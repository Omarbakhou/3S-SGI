package com.SSS.SGI.entity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable; /**
 * Classe pour la clé composite de la table Affectation
 */
@Setter
@Getter
public class AffectationId implements Serializable {
    private Long collaborateur;
    private Long projet;

    public AffectationId() {}

    public AffectationId(Long collaborateur, Long projet) {
        this.collaborateur = collaborateur;
        this.projet = projet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AffectationId that = (AffectationId) o;
        return collaborateur.equals(that.collaborateur) &&
               projet.equals(that.projet);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(collaborateur, projet);
    }

}
