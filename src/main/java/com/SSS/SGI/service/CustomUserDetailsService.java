package com.SSS.SGI.service;

import com.SSS.SGI.entity.Collaborateur;
import com.SSS.SGI.entity.Employe;
import com.SSS.SGI.entity.Manager;
import com.SSS.SGI.repository.CollaborateurRepository;
import com.SSS.SGI.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Charge un collaborateur (employé ou manager) par email pour l'authentification.
 * Il n'existe pas d'entité Admin en base (contrainte : pas de changement de schéma) :
 * un manager est promu ADMIN si son email figure dans sgi.security.admin-emails.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final CollaborateurRepository collaborateurRepository;
    private final Set<String> adminEmails;

    public CustomUserDetailsService(
            CollaborateurRepository collaborateurRepository,
            @Value("${sgi.security.admin-emails:}") String adminEmailsProperty) {
        this.collaborateurRepository = collaborateurRepository;
        this.adminEmails = Arrays.stream(adminEmailsProperty.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Collaborateur collaborateur = collaborateurRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Aucun compte pour l'email : " + email));

        List<String> roles = resolveRoles(collaborateur);
        return CustomUserDetails.fromRoles(
                collaborateur.getId(), collaborateur.getEmail(), collaborateur.getMotDePasse(), roles);
    }

    private List<String> resolveRoles(Collaborateur collaborateur) {
        if (collaborateur instanceof Manager) {
            boolean isAdmin = adminEmails.contains(collaborateur.getEmail().toLowerCase());
            return isAdmin ? List.of("MANAGER", "ADMIN") : List.of("MANAGER");
        }
        if (collaborateur instanceof Employe) {
            return List.of("EMPLOYE");
        }
        throw new UsernameNotFoundException("Type de collaborateur non reconnu pour : " + collaborateur.getEmail());
    }
}
