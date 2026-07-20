package com.SSS.SGI.exception;

/**
 * Levée quand un manager tente de valider une absence dont le type exige
 * un justificatif (maladie, maternité/paternité) et qu'aucun n'a été fourni.
 */
public class JustificatifManquantException extends RuntimeException {
    public JustificatifManquantException(String message) {
        super(message);
    }
}
