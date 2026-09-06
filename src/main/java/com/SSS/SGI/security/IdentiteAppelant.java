package com.SSS.SGI.security;

import org.springframework.security.access.AccessDeniedException;

/**
 * Résolution de l'identité de l'appelant à partir du jeton.
 *
 * Un identifiant transmis par le client — segment d'URL ou paramètre de requête — n'est jamais
 * une preuve d'identité : {@code @PreAuthorize} contrôle le rôle, pas la personne. Sans ce
 * garde-fou, un employé authentifié agit au nom de n'importe quel autre en changeant un id
 * dans l'URL, et un manager valide hors de son équipe en annonçant l'id d'un confrère.
 *
 * Les signatures d'URL existantes sont conservées pour ne pas rompre le contrat public, mais
 * l'id reçu n'est plus qu'une assertion à confronter au principal : c'est toujours l'id porté
 * par le jeton qui est transmis au service.
 */
public final class IdentiteAppelant {

    private static final String ROLE_EMPLOYE = "ROLE_EMPLOYE";
    private static final String ROLE_MANAGER = "ROLE_MANAGER";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private IdentiteAppelant() {
    }

    /**
     * Identité à retenir pour une action portant sur les données propres de l'appelant.
     *
     * @param idFourni  id transmis par le client, éventuellement nul : les paramètres d'identité
     *                  sont devenus facultatifs puisque le jeton fait foi
     * @param principal porteur du jeton
     * @return l'id du principal, jamais celui transmis par le client
     * @throws AccessDeniedException si le client prétend agir au nom de quelqu'un d'autre
     */
    public static Long resoudre(Long idFourni, CustomUserDetails principal) {
        Long idAppelant = exigerPrincipal(principal).getId();
        if (idFourni != null && !idFourni.equals(idAppelant)) {
            throw new AccessDeniedException(
                    "Vous ne pouvez agir qu'en votre propre nom : l'identifiant transmis ne correspond pas à votre compte.");
        }
        return idAppelant;
    }

    /**
     * Vérifie qu'une ressource déjà chargée appartient bien à l'appelant, lorsque l'URL désigne
     * la ressource (et non son titulaire) et que le lien de propriété n'est connu qu'en base.
     */
    public static void exigerProprietaire(Long idProprietaire, CustomUserDetails principal) {
        if (!exigerPrincipal(principal).getId().equals(idProprietaire)) {
            throw new AccessDeniedException(
                    "Cette ressource appartient à un autre collaborateur.");
        }
    }

    /**
     * Lectures ouvertes à plusieurs rôles : un employé ne consulte que son propre dossier.
     * Le périmètre plus large d'un manager ou d'un admin relève de la hiérarchie des rôles,
     * hors du champ de ce correctif.
     */
    public static void exigerProprietaireSiSimpleEmploye(Long idCible, CustomUserDetails principal) {
        if (estSimpleEmploye(exigerPrincipal(principal))) {
            exigerProprietaire(idCible, principal);
        }
    }

    private static boolean estSimpleEmploye(CustomUserDetails principal) {
        return aRole(principal, ROLE_EMPLOYE)
                && !aRole(principal, ROLE_MANAGER)
                && !aRole(principal, ROLE_ADMIN);
    }

    private static boolean aRole(CustomUserDetails principal, String role) {
        return principal.getAuthorities().stream().anyMatch(a -> role.equals(a.getAuthority()));
    }

    private static CustomUserDetails exigerPrincipal(CustomUserDetails principal) {
        // Inatteignable tant que SecurityConfig exige l'authentification sur /api/** ; garde-fou
        // pour qu'un endpoint rendu public par erreur échoue fermé plutôt qu'en NullPointerException.
        if (principal == null) {
            throw new AccessDeniedException("Authentification requise pour cette action.");
        }
        return principal;
    }
}
