package com.SSS.SGI.exception;

/**
 * Un ADMIN est le sommet de la hiérarchie de validation : il n'a pas d'approbateur,
 * il ne peut donc pas déposer de demande d'absence (seulement valider celles des managers).
 */
public class AdminNonAutoriseException extends RuntimeException {
    public AdminNonAutoriseException(String message) {
        super(message);
    }
}
