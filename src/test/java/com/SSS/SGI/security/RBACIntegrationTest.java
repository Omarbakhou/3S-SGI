package com.SSS.SGI.security;

import com.SSS.SGI.entity.Employe;
import com.SSS.SGI.entity.Manager;
import com.SSS.SGI.repository.CollaborateurRepository;
import com.SSS.SGI.repository.EmployeRepository;
import com.SSS.SGI.repository.ManagerRepository;
import com.SSS.SGI.service.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Vérifie l'application effective des rôles (ADMIN, MANAGER, EMPLOYE) sur des
 * endpoints réels du dépôt. ADMIN n'a pas d'entité dédiée (contrainte : pas de
 * changement de schéma) : un manager dont l'email figure dans
 * sgi.security.admin-emails obtient en plus le rôle ADMIN (voir CustomUserDetailsService).
 *
 * Les corps de requête sont écrits à la main (voir SecurityIntegrationTest pour le pourquoi :
 * pas d'ObjectMapper Jackson 2 disponible comme bean Spring dans ce projet Boot 4 / Jackson 3).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RBACIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@sgi.test"; // doit correspondre à application-test.properties

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
        employe.setEmail("employe.rbac@sgi.test");
        employe.setMotDePasse(passwordEncoder.encode("Password123!"));
        employe = employeRepository.save(employe);

        manager = new Manager();
        manager.setNom("Smith");
        manager.setPrenom("John");
        manager.setEmail("manager.rbac@sgi.test");
        manager.setMotDePasse(passwordEncoder.encode("Password123!"));
        manager = managerRepository.save(manager);

        adminManager = new Manager();
        adminManager.setNom("Root");
        adminManager.setPrenom("Ada");
        adminManager.setEmail(ADMIN_EMAIL);
        adminManager.setMotDePasse(passwordEncoder.encode("Password123!"));
        adminManager = managerRepository.save(adminManager);
    }

    private String tokenFor(String email) {
        UserDetails details = userDetailsService.loadUserByUsername(email);
        return "Bearer " + jwtUtil.generateToken((CustomUserDetails) details);
    }

    private static String allouerQuotaBody(Long employeId) {
        return "{\"employeId\":%d,\"typeAbsence\":\"CONGE_PAYE\",\"annee\":2026,\"joursAlloues\":5.0}"
                .formatted(employeId);
    }

    private static String updateProfileBody(String nom, String prenom, String email, String motDePasseActuel) {
        return "{\"nom\":\"%s\",\"prenom\":\"%s\",\"email\":\"%s\",\"motDePasseActuel\":\"%s\"}"
                .formatted(nom, prenom, email, motDePasseActuel);
    }

    // ---- GET /api/collaborateurs : hasRole('MANAGER') ----

    @Test
    @DisplayName("GET /api/collaborateurs : MANAGER -> 200")
    void listCollaborateurs_manager_ok() throws Exception {
        mockMvc.perform(get("/api/collaborateurs").header("Authorization", tokenFor(manager.getEmail())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/collaborateurs : manager promu ADMIN -> 200 (porte aussi ROLE_MANAGER)")
    void listCollaborateurs_admin_ok() throws Exception {
        mockMvc.perform(get("/api/collaborateurs").header("Authorization", tokenFor(ADMIN_EMAIL)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/collaborateurs : EMPLOYE -> 403")
    void listCollaborateurs_employe_forbidden() throws Exception {
        mockMvc.perform(get("/api/collaborateurs").header("Authorization", tokenFor(employe.getEmail())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/collaborateurs : sans jeton -> 401")
    void listCollaborateurs_noToken_unauthorized() throws Exception {
        mockMvc.perform(get("/api/collaborateurs"))
                .andExpect(status().isUnauthorized());
    }

    // ---- POST /api/absences/quotas : hasRole('ADMIN') ----

    @Test
    @DisplayName("POST /api/absences/quotas : ADMIN -> 201")
    void allouerQuota_admin_created() throws Exception {
        mockMvc.perform(post("/api/absences/quotas")
                        .header("Authorization", tokenFor(ADMIN_EMAIL))
                        .contentType("application/json")
                        .content(allouerQuotaBody(employe.getId())))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/absences/quotas : MANAGER (non-admin) -> 403")
    void allouerQuota_manager_forbidden() throws Exception {
        mockMvc.perform(post("/api/absences/quotas")
                        .header("Authorization", tokenFor(manager.getEmail()))
                        .contentType("application/json")
                        .content(allouerQuotaBody(employe.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/absences/quotas : EMPLOYE -> 403")
    void allouerQuota_employe_forbidden() throws Exception {
        mockMvc.perform(post("/api/absences/quotas")
                        .header("Authorization", tokenFor(employe.getEmail()))
                        .contentType("application/json")
                        .content(allouerQuotaBody(employe.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/absences/quotas : sans jeton -> 401")
    void allouerQuota_noToken_unauthorized() throws Exception {
        mockMvc.perform(post("/api/absences/quotas")
                        .contentType("application/json")
                        .content(allouerQuotaBody(employe.getId())))
                .andExpect(status().isUnauthorized());
    }

    // ---- Suppression physique d'un collaborateur : endpoint retiré ----

    @Test
    @DisplayName("DELETE /api/collaborateurs/{id} n'existe plus, même pour un manager")
    void deleteCollaborateur_endpointRetire() throws Exception {
        Employe cible = new Employe();
        cible.setNom("Temp");
        cible.setPrenom("Worker");
        cible.setEmail("temp.delete@sgi.test");
        cible.setMotDePasse(passwordEncoder.encode("Password123!"));
        cible = employeRepository.save(cible);

        // Supprimer la ligne emporterait l'historique des imputations et des absences :
        // la désactivation (AdminController) est désormais la seule voie pour retirer un accès.
        mockMvc.perform(delete("/api/collaborateurs/" + cible.getId())
                        .header("Authorization", tokenFor(manager.getEmail())))
                .andExpect(status().isMethodNotAllowed());

        assertTrue(collaborateurRepository.findById(cible.getId()).isPresent(),
                "le collaborateur doit toujours exister en base");
    }

    // ---- Création de comptes : réservée aux ADMIN ----

    @Test
    @DisplayName("POST /api/collaborateurs/manager : MANAGER -> 403, ADMIN -> 201")
    void createManager_reserveAuxAdmins() throws Exception {
        String corps = "{\"nom\":\"Nouveau\",\"prenom\":\"Manager\","
                + "\"email\":\"nouveau.manager@sgi.test\",\"motDePasse\":\"Password123!\"}";

        mockMvc.perform(post("/api/collaborateurs/manager")
                        .header("Authorization", tokenFor(manager.getEmail()))
                        .contentType("application/json")
                        .content(corps))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/collaborateurs/manager")
                        .header("Authorization", tokenFor(ADMIN_EMAIL))
                        .contentType("application/json")
                        .content(corps))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/collaborateurs/employe : MANAGER -> 403, ADMIN -> 201")
    void createEmploye_reserveAuxAdmins() throws Exception {
        String corps = "{\"nom\":\"Nouvel\",\"prenom\":\"Employe\","
                + "\"email\":\"nouvel.employe@sgi.test\",\"motDePasse\":\"Password123!\"}";

        mockMvc.perform(post("/api/collaborateurs/employe")
                        .header("Authorization", tokenFor(manager.getEmail()))
                        .contentType("application/json")
                        .content(corps))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/collaborateurs/employe")
                        .header("Authorization", tokenFor(ADMIN_EMAIL))
                        .contentType("application/json")
                        .content(corps))
                .andExpect(status().isCreated());
    }

    // ---- IDOR : PUT /api/collaborateurs/{id}/profile ----

    @Test
    @DisplayName("PUT /profile : modifier son propre profil -> 200")
    void updateProfile_ownAccount_ok() throws Exception {
        mockMvc.perform(put("/api/collaborateurs/" + employe.getId() + "/profile")
                        .header("Authorization", tokenFor(employe.getEmail()))
                        .contentType("application/json")
                        .content(updateProfileBody("Doe", "Jane", employe.getEmail(), "Password123!")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /profile : modifier le profil d'un autre utilisateur -> 403")
    void updateProfile_otherAccount_forbidden() throws Exception {
        mockMvc.perform(put("/api/collaborateurs/" + manager.getId() + "/profile")
                        .header("Authorization", tokenFor(employe.getEmail()))
                        .contentType("application/json")
                        .content(updateProfileBody("Hacked", "Name", "hacked@sgi.test", "Password123!")))
                .andExpect(status().isForbidden());
    }
}
