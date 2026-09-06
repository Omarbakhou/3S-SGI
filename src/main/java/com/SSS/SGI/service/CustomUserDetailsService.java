package com.SSS.SGI.service;

import com.SSS.SGI.entity.Collaborateur;
import com.SSS.SGI.entity.Employe;
import com.SSS.SGI.entity.Manager;
import com.SSS.SGI.repository.CollaborateurRepository;
import com.SSS.SGI.security.AdminEmails;
import com.SSS.SGI.security.CustomUserDetails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Charge un collaborateur (employé ou manager) par email pour l'authentification.
 * Il n'existe pas d'entité Admin en base (contrainte : pas de changement de schéma) :
 * un manager est promu ADMIN si son email figure dans {@link AdminEmails}.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final CollaborateurRepository collaborateurRepository;
    private final AdminEmails adminEmails;

    public CustomUserDetailsService(
            CollaborateurRepository collaborateurRepository,
            AdminEmails adminEmails) {
        this.collaborateurRepository = collaborateurRepository;
        this.adminEmails = adminEmails;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Collaborateur collaborateur = collaborateurRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Aucun compte pour l'email : " + email));

        List<String> roles = resolveRoles(collaborateur);
        // `actif` est porté jusqu'au principal : Spring Security refuse alors la
        // connexion d'un compte désactivé via une DisabledException, traduite en
        // message explicite par AuthController.
        return CustomUserDetails.fromRoles(
                collaborateur.getId(), collaborateur.getEmail(), collaborateur.getMotDePasse(),
                collaborateur.getNom(), collaborateur.getPrenom(), roles, collaborateur.isActif());
    }

    private List<String> resolveRoles(Collaborateur collaborateur) {
        if (collaborateur instanceof Manager) {
            return adminEmails.contient(collaborateur.getEmail())
                    ? List.of("MANAGER", "ADMIN")
                    : List.of("MANAGER");
        }
        if (collaborateur instanceof Employe) {
            return List.of("EMPLOYE");
        }
        throw new UsernameNotFoundException("Type de collaborateur non reconnu pour : " + collaborateur.getEmail());
    }
}
