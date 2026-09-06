package com.SSS.SGI.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Principal Spring Security pour un collaborateur (employé ou manager).
 * Porte l'id métier pour permettre les vérifications de propriété (IDOR) dans les contrôleurs,
 * et nom/prénom pour l'affichage côté frontend sans aller-retour supplémentaire en base.
 */
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String nom;
    private final String prenom;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(
            Long id, String email, String password, String nom, String prenom,
            Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.nom = nom;
        this.prenom = prenom;
        this.authorities = authorities;
    }

    public static CustomUserDetails fromRoles(
            Long id, String email, String password, String nom, String prenom, List<String> roles) {
        List<GrantedAuthority> authorities = roles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        return new CustomUserDetails(id, email, password, nom, prenom, authorities);
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getPrenom() {
        return prenom;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
