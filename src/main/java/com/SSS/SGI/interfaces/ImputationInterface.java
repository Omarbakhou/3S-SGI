package com.SSS.SGI.interfaces;

import com.SSS.SGI.entity.StatutImputation;

/**
 * Interface définissant le comportement d'une imputation
 * Propriétés : nom, nom du projet, nom de l'employé, statut
 */
public interface ImputationInterface {

    String getNomImputation();

    void setNomImputation(String nom);

    String getNomProjet();

    String getNomEmploye();

    StatutImputation getStatutImputation();

    void setStatutImputation(StatutImputation statut);
}

