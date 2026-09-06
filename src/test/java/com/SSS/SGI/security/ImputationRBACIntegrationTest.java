package com.SSS.SGI.security;

import com.SSS.SGI.entity.Affectation;
import com.SSS.SGI.entity.Client;
import com.SSS.SGI.entity.Employe;
import com.SSS.SGI.entity.Imputation;
import com.SSS.SGI.entity.Manager;
import com.SSS.SGI.entity.Projet;
import com.SSS.SGI.entity.StatutImputation;
import com.SSS.SGI.repository.AffectationRepository;
import com.SSS.SGI.repository.ClientRepository;
import com.SSS.SGI.repository.EmployeRepository;
import com.SSS.SGI.repository.ImputationRepository;
import com.SSS.SGI.repository.ManagerRepository;
import com.SSS.SGI.repository.ProjetRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Couverture RBAC des endpoints /api/imputations : rôles, légitimité manager (IDOR)
 * et validation des DTOs, sur le modèle de RBACIntegrationTest (module Absence).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ImputationRBACIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EmployeRepository employeRepository;
    @Autowired private ManagerRepository managerRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private ProjetRepository projetRepository;
    @Autowired private AffectationRepository affectationRepository;
    @Autowired private ImputationRepository imputationRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private CustomUserDetailsService userDetailsService;

    private Employe employe;
    private Manager manager;
    private Manager autreManager;
    private Projet projet;

    @BeforeEach
    void setUp() {
        manager = new Manager();
        manager.setNom("Smith");
        manager.setPrenom("John");
        manager.setEmail("manager.imputation@sgi.test");
        manager.setMotDePasse(passwordEncoder.encode("Password123!"));
        manager = managerRepository.save(manager);

        autreManager = new Manager();
        autreManager.setNom("Autre");
        autreManager.setPrenom("Manager");
        autreManager.setEmail("autre.manager.imputation@sgi.test");
        autreManager.setMotDePasse(passwordEncoder.encode("Password123!"));
        autreManager = managerRepository.save(autreManager);

        employe = new Employe();
        employe.setNom("Doe");
        employe.setPrenom("Jane");
        employe.setEmail("employe.imputation@sgi.test");
        employe.setMotDePasse(passwordEncoder.encode("Password123!"));
        employe.setManager(manager);
        employe = employeRepository.save(employe);

        Client client = new Client();
        client.setNomClient("Client RBAC Test");
        client = clientRepository.save(client);

        projet = new Projet();
        projet.setNom("Projet RBAC Test");
        projet.setClient(client);
        projet = projetRepository.save(projet);

        Affectation affectation = new Affectation();
        affectation.setCollaborateur(employe);
        affectation.setProjet(projet);
        affectation.setTauxAffectation(new BigDecimal("50.00"));
        affectation.setDateAffectation(LocalDate.now());
        affectationRepository.save(affectation);
    }

    private String tokenFor(String email) {
        UserDetails details = userDetailsService.loadUserByUsername(email);
        return "Bearer " + jwtUtil.generateToken((CustomUserDetails) details);
    }

    private Imputation creerImputationEnAttente() {
        Imputation i = new Imputation();
        i.setNom("Développement");
        i.setStatut(StatutImputation.EN_ATTENTE);
        i.setEmploye(employe);
        i.setProjet(projet);
        i.setDateImputation(LocalDate.now());
        i.setHeures(5.0);
        return imputationRepository.save(i);
    }

    private static String createBody(Long projetId, String date, double heures, String nom) {
        return "{\"projetId\":%d,\"dateImputation\":\"%s\",\"heures\":%s,\"nom\":\"%s\"}"
                .formatted(projetId, date, heures, nom);
    }

    // ---- POST /api/imputations/employe/{id} : hasRole('EMPLOYE') ----

    @Test
    @DisplayName("POST /employe/{id} : EMPLOYE affecté au projet -> 201")
    void creer_employe_created() throws Exception {
        mockMvc.perform(post("/api/imputations/employe/" + employe.getId())
                        .header("Authorization", tokenFor(employe.getEmail()))
                        .contentType("application/json")
                        .content(createBody(projet.getId(), LocalDate.now().toString(), 6.0, "Dev API")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"));
    }

    @Test
    @DisplayName("POST /employe/{id} : MANAGER -> 403")
    void creer_manager_forbidden() throws Exception {
        mockMvc.perform(post("/api/imputations/employe/" + employe.getId())
                        .header("Authorization", tokenFor(manager.getEmail()))
                        .contentType("application/json")
                        .content(createBody(projet.getId(), LocalDate.now().toString(), 6.0, "Dev API")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /employe/{id} : sans jeton -> 401")
    void creer_noToken_unauthorized() throws Exception {
        mockMvc.perform(post("/api/imputations/employe/" + employe.getId())
                        .contentType("application/json")
                        .content(createBody(projet.getId(), LocalDate.now().toString(), 6.0, "Dev API")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /employe/{id} : employé non affecté au projet -> 400")
    void creer_employeNonAffecte_badRequest() throws Exception {
        Projet autreProjet = new Projet();
        autreProjet.setNom("Projet sans affectation");
        autreProjet.setClient(projet.getClient());
        autreProjet = projetRepository.save(autreProjet);

        mockMvc.perform(post("/api/imputations/employe/" + employe.getId())
                        .header("Authorization", tokenFor(employe.getEmail()))
                        .contentType("application/json")
                        .content(createBody(autreProjet.getId(), LocalDate.now().toString(), 6.0, "Dev API")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /employe/{id} : heures à 0 -> 400")
    void creer_heuresInvalides_badRequest() throws Exception {
        mockMvc.perform(post("/api/imputations/employe/" + employe.getId())
                        .header("Authorization", tokenFor(employe.getEmail()))
                        .contentType("application/json")
                        .content(createBody(projet.getId(), LocalDate.now().toString(), 0.0, "Dev API")))
                .andExpect(status().isBadRequest());
    }

    // ---- GET /api/imputations : hasRole('MANAGER') ----

    @Test
    @DisplayName("GET /api/imputations : MANAGER -> 200, EMPLOYE -> 403")
    void listAll_roleEnforced() throws Exception {
        mockMvc.perform(get("/api/imputations").header("Authorization", tokenFor(manager.getEmail())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/imputations").header("Authorization", tokenFor(employe.getEmail())))
                .andExpect(status().isForbidden());
    }

    // ---- GET /api/imputations/employe/{id} : hasAnyRole('EMPLOYE','MANAGER') ----

    @Test
    @DisplayName("GET /employe/{id} : EMPLOYE (soi-même) -> 200, MANAGER -> 200")
    void listByEmploye_ok() throws Exception {
        creerImputationEnAttente();

        mockMvc.perform(get("/api/imputations/employe/" + employe.getId())
                        .header("Authorization", tokenFor(employe.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeId").value(employe.getId()));

        mockMvc.perform(get("/api/imputations/employe/" + employe.getId())
                        .header("Authorization", tokenFor(manager.getEmail())))
                .andExpect(status().isOk());
    }

    // ---- GET /api/imputations/en-attente : scoping manager (IDOR-like) ----

    @Test
    @DisplayName("GET /en-attente : ne retourne que les imputations des employés du manager appelant")
    void enAttente_scopeParManager() throws Exception {
        creerImputationEnAttente();

        mockMvc.perform(get("/api/imputations/en-attente").param("managerId", String.valueOf(manager.getId()))
                        .header("Authorization", tokenFor(manager.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/imputations/en-attente").param("managerId", String.valueOf(autreManager.getId()))
                        .header("Authorization", tokenFor(autreManager.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---- POST /{id}/valider : légitimité manager ----

    @Test
    @DisplayName("POST /valider : manager légitime -> 200, manager non légitime -> 403")
    void valider_legitimiteManager() throws Exception {
        Imputation imputation = creerImputationEnAttente();

        mockMvc.perform(post("/api/imputations/" + imputation.getId() + "/valider")
                        .param("managerId", String.valueOf(autreManager.getId()))
                        .header("Authorization", tokenFor(autreManager.getEmail())))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/imputations/" + imputation.getId() + "/valider")
                        .param("managerId", String.valueOf(manager.getId()))
                        .header("Authorization", tokenFor(manager.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("VALIDEE"));
    }

    // ---- POST /{id}/rejeter : motif obligatoire ----

    @Test
    @DisplayName("POST /rejeter : sans motif -> 400, avec motif -> 200 et motif enregistré")
    void rejeter_motifObligatoire() throws Exception {
        Imputation imputation = creerImputationEnAttente();

        mockMvc.perform(post("/api/imputations/" + imputation.getId() + "/rejeter")
                        .param("managerId", String.valueOf(manager.getId()))
                        .header("Authorization", tokenFor(manager.getEmail()))
                        .contentType("application/json")
                        .content("{\"motif\":\"\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/imputations/" + imputation.getId() + "/rejeter")
                        .param("managerId", String.valueOf(manager.getId()))
                        .header("Authorization", tokenFor(manager.getEmail()))
                        .contentType("application/json")
                        .content("{\"motif\":\"Description trop vague\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("REJETEE"))
                .andExpect(jsonPath("$.motifRejet").value("Description trop vague"));
    }

    // ---- GET /projet/{id}/cumul-heures : hasAnyRole('MANAGER','ADMIN') ----

    @Test
    @DisplayName("GET /cumul-heures : MANAGER -> 200, EMPLOYE -> 403, sans jeton -> 401")
    void cumulHeures_roleEnforced() throws Exception {
        mockMvc.perform(get("/api/imputations/projet/" + projet.getId() + "/cumul-heures")
                        .header("Authorization", tokenFor(manager.getEmail())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/imputations/projet/" + projet.getId() + "/cumul-heures")
                        .header("Authorization", tokenFor(employe.getEmail())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/imputations/projet/" + projet.getId() + "/cumul-heures"))
                .andExpect(status().isUnauthorized());
    }
}
