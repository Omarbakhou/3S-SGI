package com.SSS.SGI.interfaces;

import com.SSS.SGI.entity.Manager;

/**
 * Contrat de validation d'une absence par un Manager.
 * Distinct de ValidationInterface (Imputation) car le rejet d'une absence
 * porte systématiquement un motif.
 */
public interface ValidationAbsenceInterface {
    void valider(Manager manager);
    void rejeter(Manager manager, String motif);
    boolean peutEtreValidee();
}
