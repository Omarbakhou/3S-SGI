package com.SSS.SGI.entity.enums;

/**
 * Types d'absence gérés par le SGI.
 * Chaque type porte ses propres règles de gestion :
 *  - soumisAQuota   : décompte des jours restants (QuotaAbsence) à la validation
 *  - justificatifObligatoire : un justificatif doit être attaché avant validation
 */
public enum TypeAbsence {

    CONGE_PAYE(true, false),
    RTT(true, false),
    CONGE_EXCEPTIONNEL(true, false),
    MALADIE(false, true),
    CONGE_MATERNITE_PATERNITE(false, true),
    SANS_SOLDE(false, false);

    private final boolean soumisAQuota;
    private final boolean justificatifObligatoire;

    TypeAbsence(boolean soumisAQuota, boolean justificatifObligatoire) {
        this.soumisAQuota = soumisAQuota;
        this.justificatifObligatoire = justificatifObligatoire;
    }

    public boolean isSoumisAQuota() {
        return soumisAQuota;
    }

    public boolean isJustificatifObligatoire() {
        return justificatifObligatoire;
    }
}
