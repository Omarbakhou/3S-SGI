package com.SSS.SGI.interfaces;

import com.SSS.SGI.entity.Manager;
import java.time.LocalDateTime;

/**
 * Interface définissant le comportement de validation d'une imputation
 * Permet de valider ou rejeter une imputation
 */
public interface ValidationInterface {

    /**
     * Valide l'imputation
     *
     * @param manager le manager qui valide l'imputation
     */
    void valider(Manager manager);

    /**
     * Rejette l'imputation
     *
     * @param manager le manager qui rejette l'imputation
     */
    void rejeter(Manager manager);

    /**
     * Retourne le manager validateur
     */
    Manager getManagerValidateur();

    /**
     * Retourne la date de validation
     */
    LocalDateTime getDateValidation();
}

