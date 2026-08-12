package com.SSS.SGI.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET = "testSecretKeyForJWTTokenGenerationAndValidationMustBeAtLeast256Bit";

    private JwtUtil jwtUtil(long expirationMs) {
        return new JwtUtil(SECRET, expirationMs);
    }

    private CustomUserDetails employeUser() {
        return CustomUserDetails.fromRoles(1L, "employe@sgi.test", "hash", List.of("EMPLOYE"));
    }

    @Test
    @DisplayName("Un jeton généré est valide et porte l'email, l'id et les rôles")
    void generateToken_roundTrip() {
        JwtUtil jwtUtil = jwtUtil(60_000);
        CustomUserDetails user = employeUser();

        String token = jwtUtil.generateToken(user);

        assertTrue(jwtUtil.isTokenValid(token));
        assertEquals("employe@sgi.test", jwtUtil.extractEmail(token));
        assertEquals(1L, jwtUtil.extractId(token));
        assertEquals(List.of("EMPLOYE"), jwtUtil.extractRoles(token));
    }

    @Test
    @DisplayName("Un jeton expiré est rejeté")
    void expiredToken_isInvalid() {
        JwtUtil jwtUtil = jwtUtil(-1_000);
        String token = jwtUtil.generateToken(employeUser());

        assertFalse(jwtUtil.isTokenValid(token));
        assertThrows(Exception.class, () -> jwtUtil.parseClaims(token));
    }

    @Test
    @DisplayName("Un jeton signé avec une autre clé est rejeté")
    void tokenSignedWithDifferentSecret_isInvalid() {
        JwtUtil issuer = jwtUtil(60_000);
        String token = issuer.generateToken(employeUser());

        JwtUtil verifier = new JwtUtil("differentSecretKeyForJWTValidationMustBeAtLeast256BitLongToo", 60_000);

        assertFalse(verifier.isTokenValid(token));
    }

    @Test
    @DisplayName("Un jeton altéré (signature invalide) est rejeté")
    void tamperedToken_isInvalid() {
        JwtUtil jwtUtil = jwtUtil(60_000);
        String token = jwtUtil.generateToken(employeUser());

        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertFalse(jwtUtil.isTokenValid(tampered));
    }

    @Test
    @DisplayName("Une chaîne qui n'est pas un JWT est rejetée")
    void garbageToken_isInvalid() {
        JwtUtil jwtUtil = jwtUtil(60_000);

        assertFalse(jwtUtil.isTokenValid("not-a-jwt"));
    }

    @Test
    @DisplayName("Les rôles multiples sont conservés dans le jeton (cas manager promu admin)")
    void multipleRoles_arePreserved() {
        JwtUtil jwtUtil = jwtUtil(60_000);
        CustomUserDetails adminManager = CustomUserDetails.fromRoles(2L, "admin@sgi.test", "hash", List.of("MANAGER", "ADMIN"));

        String token = jwtUtil.generateToken(adminManager);

        assertEquals(List.of("MANAGER", "ADMIN"), jwtUtil.extractRoles(token));
    }
}
