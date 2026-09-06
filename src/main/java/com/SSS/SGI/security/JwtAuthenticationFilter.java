package com.SSS.SGI.security;

import com.SSS.SGI.entity.Collaborateur;
import com.SSS.SGI.repository.CollaborateurRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Extrait et valide le JWT du header Authorization, puis peuple le SecurityContext.
 * Ne rejette jamais la requête elle-même : un jeton absent/invalide laisse le contexte
 * vide, et c'est la règle d'autorisation (authenticated()/permitAll()) qui décide du 401/403.
 *
 * <p>Le filtre revérifie en base que le compte est toujours actif. Sans cela, un jeton
 * émis avant une désactivation resterait utilisable jusqu'à son expiration (24 h) :
 * la désactivation ne serait donc pas immédiate. Le coût est un SELECT par requête
 * authentifiée, assumé pour que retirer un accès prenne effet tout de suite.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final CollaborateurRepository collaborateurRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, CollaborateurRepository collaborateurRepository) {
        this.jwtUtil = jwtUtil;
        this.collaborateurRepository = collaborateurRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                String email = jwtUtil.extractEmail(token);
                Long id = jwtUtil.extractId(token);
                List<String> roles = jwtUtil.extractRoles(token);
                String nom = jwtUtil.extractNom(token);
                String prenom = jwtUtil.extractPrenom(token);

                // Un compte désactivé (ou supprimé) depuis l'émission du jeton ne doit plus
                // être authentifié : on laisse le contexte vide, la règle d'autorisation
                // renverra 401 comme pour un jeton invalide.
                boolean actif = collaborateurRepository.findByEmail(email)
                        .map(Collaborateur::isActif)
                        .orElse(false);

                if (actif) {
                    CustomUserDetails principal =
                            CustomUserDetails.fromRoles(id, email, null, nom, prenom, roles, true);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    SecurityContextHolder.clearContext();
                }
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
