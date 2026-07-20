package com.SSS.SGI.exception;

/**
 * Levée quand le quota restant de l'employé pour un type d'absence donné
 * ne couvre pas la période demandée. À mapper en 400 Bad Request dans
 * GlobalExceptionHandler (cf. INTEGRATION.md).
 */
public class QuotaInsuffisantException extends RuntimeException {
    public QuotaInsuffisantException(String message) {
        super(message);
    }
}
