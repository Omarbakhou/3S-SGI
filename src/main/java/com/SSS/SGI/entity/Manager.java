package com.SSS.SGI.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "manager")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = {"imputationsValidees"})
@ToString(callSuper = true, exclude = {"imputationsValidees"})
@DiscriminatorValue("MANAGER")
public class Manager extends Collaborateur {

    @OneToMany(mappedBy = "managerValidateur", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.LAZY)
    private Set<Imputation> imputationsValidees = new HashSet<>();

    /**
     * Valide une imputation
     *
     * @param imputation l'imputation à valider
     * @throws IllegalStateException si l'imputation n'est pas en attente
     */
    public void validerImputation(Imputation imputation) {
        if (imputation.getStatut() != StatutImputation.EN_ATTENTE) {
            throw new IllegalStateException("Seules les imputations en attente peuvent être validées");
        }
        imputation.valider(this);
    }

    /**
     * Rejette une imputation
     *
     * @param imputation l'imputation à rejeter
     * @throws IllegalStateException si l'imputation n'est pas en attente
     */
    public void rejeterImputation(Imputation imputation) {
        if (imputation.getStatut() != StatutImputation.EN_ATTENTE) {
            throw new IllegalStateException("Seules les imputations en attente peuvent être rejetées");
        }
        imputation.rejeter(this);
    }

    /**
     * Ajoute une imputation validée
     */
    public void addImputationValidee(Imputation imputation) {
        imputationsValidees.add(imputation);
        imputation.setManagerValidateur(this);
    }

    /**
     * Retire une imputation validée
     */
    public void removeImputationValidee(Imputation imputation) {
        imputationsValidees.remove(imputation);
    }
}

