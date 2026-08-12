package com.SSS.SGI.security;

import com.SSS.SGI.entity.Employe;
import com.SSS.SGI.repository.EmployeRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flux d'authentification bout en bout : login, /me, /refresh, et rejet des
 * requêtes sans jeton (ou avec un jeton invalide) sur les endpoints protégés.
 *
 * Les corps de requête sont écrits à la main (pas d'ObjectMapper injecté) : Spring Boot 4
 * n'expose par défaut qu'un ObjectMapper Jackson 3 (tools.jackson.databind), incompatible
 * avec le com.fasterxml.jackson.databind utilisé ailleurs (jjwt-jackson). Les réponses sont
 * lues via JsonPath, déjà présent en dépendance de test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityIntegrationTest {

    private static final String RAW_PASSWORD = "Password123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private EmployeRepository employeRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Employe employe;

    @BeforeEach
    void setUp() {
        employe = new Employe();
        employe.setNom("Doe");
        employe.setPrenom("Jane");
        employe.setEmail("jane.doe@sgi.test");
        employe.setMotDePasse(passwordEncoder.encode(RAW_PASSWORD));
        employe = employeRepository.save(employe);
    }

    private static String loginBody(String email, String password) {
        return "{\"email\":\"%s\",\"motDePasse\":\"%s\"}".formatted(email, password);
    }

    @Test
    @DisplayName("Login avec identifiants valides retourne un jeton portant les rôles")
    void login_validCredentials_returnsToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginBody(employe.getEmail(), RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.roles", hasItem("EMPLOYE")))
                .andExpect(jsonPath("$.email", is(employe.getEmail())));
    }

    @Test
    @DisplayName("Login avec mauvais mot de passe retourne 401")
    void login_wrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginBody(employe.getEmail(), "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Login avec email inconnu retourne 401 (pas 404, pour ne pas révéler l'existence du compte)")
    void login_unknownEmail_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginBody("nobody@sgi.test", "whatever")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un endpoint protégé sans jeton retourne 401")
    void protectedEndpoint_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/collaborateurs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un endpoint protégé avec un jeton invalide retourne 401")
    void protectedEndpoint_invalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/collaborateurs").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/api/auth/me retourne l'identité portée par le jeton")
    void me_returnsAuthenticatedIdentity() throws Exception {
        String token = login(employe.getEmail(), RAW_PASSWORD);

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(employe.getId().intValue())))
                .andExpect(jsonPath("$.email", is(employe.getEmail())))
                .andExpect(jsonPath("$.roles", hasItem("EMPLOYE")));
    }

    @Test
    @DisplayName("/api/auth/refresh avec un jeton valide émet un nouveau jeton")
    void refresh_validToken_returnsNewToken() throws Exception {
        String token = login(employe.getEmail(), RAW_PASSWORD);

        mockMvc.perform(post("/api/auth/refresh").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())));
    }

    @Test
    @DisplayName("/api/auth/refresh sans jeton retourne 401")
    void refresh_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginBody(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }
}
