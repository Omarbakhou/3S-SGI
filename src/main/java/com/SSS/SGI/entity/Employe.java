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
@Table(name = "employe")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = {"imputations"})
@ToString(callSuper = true, exclude = {"imputations"})
@DiscriminatorValue("EMPLOYE")
public class Employe extends Collaborateur {

    @OneToMany(mappedBy = "employe", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Imputation> imputations = new HashSet<>();

    /**
     * Ajoute une imputation
     */
    public void addImputation(Imputation imputation) {
        imputations.add(imputation);
        imputation.setEmploye(this);
    }

    /**
     * Retire une imputation
     */
    public void removeImputation(Imputation imputation) {
        imputations.remove(imputation);
        imputation.setEmploye(null);
    }
}

