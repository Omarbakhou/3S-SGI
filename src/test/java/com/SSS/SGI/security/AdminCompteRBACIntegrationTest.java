package com.SSS.SGI.security;

import com.SSS.SGI.entity.Employe;
import com.SSS.SGI.entity.Manager;
import com.SSS.SGI.repository.CollaborateurRepository;
import com.SSS.SGI.repository.EmployeRepository;
import com.SSS.SGI.repository.ManagerRepository;
import com.SSS.SGI.service.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Désactivation de comptes de bout en bout : contrôle du rôle ADMIN, garde-fous,
 * et effet réel sur la connexion.
 *
 * <p>Les corps de requête sont écrits à la main, comme dans les autres tests de
 * sécurité du dépôt (pas d'ObjectMapper Jackson 2 exposé en bean sur ce projet
 * Boot 4 / Jackson 3).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Administration des comptes (RBAC)")
class AdminCompteRBACIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@sgi.test"; // doit correspondre à application-test.properties
    private static final String MOT_DE_PASSE = "Password123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private EmployeRepository employeRepository;
    @Autowired private ManagerRepository managerRepository;
    @Autowired private CollaborateurRepository collaborateurRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private CustomUserDetailsService userDetailsService;

    private Employe employe;
    private Manager manager;
    private Manager adminManager;

    @BeforeEach
    void setUp() {
        employe = new Employe();
        employe.setNom("Doe");
        employe.setPrenom("Jane");
        employe.setEmail("employe.comptes@sgi.test");
        employe.setMotDePasse(passwordEncoder.encode(MOT_DE_PASSE));
        employe = employeRepository.save(employe);

        manager = new Manager();
        manager.setNom("Smith");
        manager.setPrenom("John");
        manager.setEmail("manager.comptes@sgi.test");
        manager.setMotDePasse(passwordEncoder.encode(MOT_DE_PASSE));
        manager = managerRepository.save(manager);

        adminManager = new Manager();
        adminManager.setNom("Root");
        adminManager.setPrenom("Ada");
        adminManager.setEmail(ADMIN_EMAIL);
        adminManager.setMotDePasse(passwordEncoder.encode(MOT_DE_PASSE));
        adminManager = managerRepository.save(adminManager);
    }

    private String tokenFor(String email) {
        UserDetails details = userDetailsService.loadUserByUsername(email);
        return "Bearer " + jwtUtil.generateToken((CustomUserDetails) details);
    }

    private static String loginBody(String email, String motDePasse) {
        return "{\"email\":\"%s\",\"motDePasse\":\"%s\"}".formatted(email, motDePasse);
    }

    @Nested
    @DisplayName("Contrôle du rôle")
    class ControleDuRole {

        @Test
        @DisplayName("un employé ne peut pas désactiver de compte (403)")
        void employe_forbidden() throws Exception {
            mockMvc.perform(patch("/api/admin/comptes/{id}/desactiver", manager.getId())
                            .header("Authorization", tokenFor(employe.getEmail())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("un manager non-admin ne peut pas désactiver de compte (403)")
        void manager_forbidden() throws Exception {
            mockMvc.perform(patch("/api/admin/comptes/{id}/desactiver", employe.getId())
                            .header("Authorization", tokenFor(manager.getEmail())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("sans jeton, l'accès est refusé (401)")
        void anonyme_unauthorized() throws Exception {
            mockMvc.perform(patch("/api/admin/comptes/{id}/desactiver", employe.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("un admin désactive un compte (200) et la ligne reste en base")
        void admin_ok() throws Exception {
            mockMvc.perform(patch("/api/admin/comptes/{id}/desactiver", employe.getId())
                            .header("Authorization", tokenFor(ADMIN_EMAIL)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.actif").value(false))
                    .andExpect(jsonPath("$.email").value(employe.getEmail()));

            assertTrue(collaborateurRepository.findById(employe.getId()).isPresent(),
                    "le compte ne doit pas être supprimé physiquement");
            assertFalse(collaborateurRepository.findById(employe.getId()).orElseThrow().isActif());
        }

        @Test
        @DisplayName("un admin réactive un compte (200)")
        void admin_reactive() throws Exception {
            mockMvc.perform(patch("/api/admin/comptes/{id}/desactiver", employe.getId())
                    .header("Authorization", tokenFor(ADMIN_EMAIL)));

            mockMvc.perform(patch("/api/admin/comptes/{id}/reactiver", employe.getId())
                            .header("Authorization", tokenFor(ADMIN_EMAIL)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.actif").value(true));
        }
    }

    @Nested
    @DisplayName("Garde-fous")
    class GardeFous {

        @Test
        @DisplayName("un admin ne peut pas désactiver son propre compte (409)")
        void autoDesactivation_conflict() throws Exception {
            mockMvc.perform(patch("/api/admin/comptes/{id}/desactiver", adminManager.getId())
                            .header("Authorization", tokenFor(ADMIN_EMAIL)))
                    .andExpect(status().isConflict());

            assertTrue(collaborateurRepository.findById(adminManager.getId()).orElseThrow().isActif());
        }

        @Test
        @DisplayName("un compte inexistant renvoie 404")
        void compteInconnu_notFound() throws Exception {
            mockMvc.perform(patch("/api/admin/comptes/{id}/desactiver", 999999)
                            .header("Authorization", tokenFor(ADMIN_EMAIL)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Effet sur l'accès")
    class EffetSurAcces {

        @Test
        @DisplayName("un compte désactivé ne peut plus se connecter, avec un message explicite")
        void connexionRefusee() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType("application/json")
                            .content(loginBody(employe.getEmail(), MOT_DE_PASSE)))
                    .andExpect(status().isOk());

            mockMvc.perform(patch("/api/admin/comptes/{id}/desactiver", employe.getId())
                            .header("Authorization", tokenFor(ADMIN_EMAIL)))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/auth/login")
                            .contentType("application/json")
                            .content(loginBody(employe.getEmail(), MOT_DE_PASSE)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("désactivé")));
        }

        @Test
        @DisplayName("un jeton émis avant la désactivation cesse immédiatement de fonctionner")
        void jetonAnterieur_revoqueImmediatement() throws Exception {
            String jetonEmploye = tokenFor(employe.getEmail());

            mockMvc.perform(get("/api/auth/me").header("Authorization", jetonEmploye))
                    .andExpect(status().isOk());

            mockMvc.perform(patch("/api/admin/comptes/{id}/desactiver", employe.getId())
                            .header("Authorization", tokenFor(ADMIN_EMAIL)))
                    .andExpect(status().isOk());

            // Sans revérification en base dans le filtre JWT, ce jeton resterait valable 24 h.
            mockMvc.perform(get("/api/auth/me").header("Authorization", jetonEmploye))
                    .andExpect(status().isUnauthorized());
        }
    }
}
