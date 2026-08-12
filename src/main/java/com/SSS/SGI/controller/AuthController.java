package com.SSS.SGI.controller;

import com.SSS.SGI.dto.LoginRequest;
import com.SSS.SGI.dto.LoginResponse;
import com.SSS.SGI.dto.MeResponse;
import com.SSS.SGI.security.CustomUserDetails;
import com.SSS.SGI.security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@Tag(name = "Authentification", description = "Login, rafraîchissement de jeton et utilisateur courant")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @Operation(summary = "Se connecter", description = "Authentifie un collaborateur et retourne un jeton JWT portant ses rôles.")
    @ApiResponse(responseCode = "200", description = "Authentification réussie")
    @ApiResponse(responseCode = "401", description = "Identifiants invalides")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.motDePasse()));
        } catch (AuthenticationException e) {
            // Message générique volontaire : ne pas révéler si c'est l'email ou le mot de passe qui est faux.
            throw new BadCredentialsException("Email ou mot de passe incorrect");
        }

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        return ResponseEntity.ok(buildLoginResponse(principal));
    }

    @Operation(summary = "Rafraîchir le jeton", description = "Émet un nouveau jeton à partir d'un jeton encore valide.")
    @ApiResponse(responseCode = "200", description = "Nouveau jeton émis")
    @ApiResponse(responseCode = "401", description = "Jeton absent, invalide ou expiré")
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new BadCredentialsException("Jeton manquant");
        }
        String token = authorizationHeader.startsWith("Bearer ")
                ? authorizationHeader.substring(7)
                : authorizationHeader;

        if (!jwtUtil.isTokenValid(token)) {
            throw new BadCredentialsException("Jeton invalide ou expiré");
        }

        CustomUserDetails principal = CustomUserDetails.fromRoles(
                jwtUtil.extractId(token), jwtUtil.extractEmail(token), null, jwtUtil.extractRoles(token));
        return ResponseEntity.ok(buildLoginResponse(principal));
    }

    @Operation(summary = "Utilisateur courant", description = "Retourne l'identité et les rôles associés au jeton fourni.")
    @ApiResponse(responseCode = "200", description = "Utilisateur courant")
    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(Authentication authentication) {
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        return ResponseEntity.ok(new MeResponse(principal.getId(), principal.getUsername(), extractRoles(principal)));
    }

    private LoginResponse buildLoginResponse(CustomUserDetails principal) {
        String token = jwtUtil.generateToken(principal);
        return new LoginResponse(
                token, "Bearer", principal.getId(), principal.getUsername(), extractRoles(principal), jwtUtil.getExpirationMs());
    }

    private List<String> extractRoles(CustomUserDetails principal) {
        return principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.replaceFirst("^ROLE_", ""))
                .toList();
    }
}
