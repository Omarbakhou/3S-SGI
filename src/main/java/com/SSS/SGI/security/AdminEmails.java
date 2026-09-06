package com.SSS.SGI.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Liste des emails de managers promus ADMIN, lue depuis la propriété
 * {@code sgi.security.admin-emails} (alimentée par la variable d'environnement
 * ADMIN_EMAILS).
 *
 * <p>Il n'existe pas d'entité Admin en base : le rôle ADMIN est accordé au runtime
 * à un Manager dont l'email figure dans cette liste. La comparaison se fait en
 * minuscules pour éviter qu'une différence de casse ne prive un administrateur
 * de ses droits.
 *
 * <p>Extrait de {@link com.SSS.SGI.service.CustomUserDetailsService} pour être
 * partagé avec les règles de désactivation de comptes, qui doivent savoir qui est
 * administrateur pour refuser la désactivation du dernier d'entre eux.
 */
@Component
public class AdminEmails {

    private final Set<String> emails;

    public AdminEmails(@Value("${sgi.security.admin-emails:}") String adminEmailsProperty) {
        this.emails = Arrays.stream(adminEmailsProperty.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    /** Indique si cet email confère le rôle ADMIN. */
    public boolean contient(String email) {
        return email != null && emails.contains(email.toLowerCase());
    }

    /** Les emails administrateurs, en minuscules. Jamais null, éventuellement vide. */
    public Set<String> emails() {
        return emails;
    }
}
