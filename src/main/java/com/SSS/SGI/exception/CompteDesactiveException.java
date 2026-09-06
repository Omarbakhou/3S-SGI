package com.SSS.SGI.exception;

/**
 * Les identifiants sont valides mais le compte a été désactivé par un administrateur.
 * Distingué des mauvais identifiants pour que l'utilisateur comprenne qu'il ne s'agit
 * pas d'une faute de frappe. Traduit en 403 par le GlobalExceptionHandler.
 */
public class CompteDesactiveException extends RuntimeException {
    public CompteDesactiveException(String message) {
        super(message);
    }
}
