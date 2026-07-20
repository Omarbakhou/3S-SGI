package com.SSS.SGI.exception;

/**
 * Levée quand une nouvelle demande d'absence chevauche une absence
 * existante (non rejetée, non annulée) du même employé.
 */
public class AbsenceChevauchementException extends RuntimeException {
    public AbsenceChevauchementException(String message) {
        super(message);
    }
}
