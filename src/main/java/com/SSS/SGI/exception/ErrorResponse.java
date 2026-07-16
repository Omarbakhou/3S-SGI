package com.SSS.SGI.exception;

import java.time.LocalDateTime;

/**
 * Classe pour la réponse d'erreur
 */
public record ErrorResponse(int status, String message, LocalDateTime timestamp) {

}

