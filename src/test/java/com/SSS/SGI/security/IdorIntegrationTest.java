package com.SSS.SGI.security;

import com.SSS.SGI.entity.Absence;
import com.SSS.SGI.entity.Affectation;
import com.SSS.SGI.entity.Client;
import com.SSS.SGI.entity.Employe;
import com.SSS.SGI.entity.Imputation;
import com.SSS.SGI.entity.Manager;
import com.SSS.SGI.entity.Projet;
import com.SSS.SGI.entity.StatutImputation;
import com.SSS.SGI.entity.enums.StatutAbsence;
import com.SSS.SGI.entity.enums.TypeAbsence;
import com.SSS.SGI.repository.AbsenceRepository;
import com.SSS.SGI.repository.AffectationRepository;
import com.SSS.SGI.repository.ClientRepository;
import com.SSS.SGI.repository.EmployeRepository;
import com.SSS.SGI.repository.ImputationRepository;
import com.SSS.SGI.repository.ManagerRepository;
import com.SSS.SGI.repository.ProjetRepository;
import com.SSS.SGI.service.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
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
 * Durcissement de l'identité de l'appelant (IDOR).
 *
 * Avant ce lot, l'identité provenait d'un @PathVariable ou d'un @RequestParam : @PreAuthorize
 * contrôlait le rôle mais jamais la personne, si bien qu'un employé agissait au nom d'un autre
 * et qu'un manager validait hors de son équipe en annonçant l'id d'un confrère.
 *
 * Chaque endpoint corrigé est couvert par un cas négatif (l'usurpation renvoie 403) doublé d'un
 * cas positif (le titulaire légitime passe toujours) : sans ce second garde-fou, un endpoint
 * cassé passerait pour un endpoint sécurisé.
 *
 * Deux équipes distinctes sont montées : employeA sous managerA, employeB sous managerB. Toute
 * action croisée doit échouer.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class IdorIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EmployeRepository employeRepository;
    @Autowired private ManagerRepository managerRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private ProjetRepository projetRepository;
    @Autowired private AffectationRepository affectationRepository;
    @Autowired private ImputationRepository imputationRepository;
    @Autowired private AbsenceRepository absenceRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private CustomUserDetailsService userDetailsService;

    private Employe employeA;
    private Employe employeB;
    private Manager managerA;
    private Manager managerB;
    private Projet projet;

    @BeforeEach
    void setUp() {
        managerA = creerManager("manager.a.idor@sgi.test");
        managerB = creerManager("manager.b.idor@sgi.test");
        employeA = creerEmploye("employe.a.idor@sgi.test", managerA);
        employeB = creerEmploye("employe.b.idor@sgi.test", managerB);

        Client client = new Client();
        client.setNomClient("Client IDOR Test");
        client = clientRepository.save(client);

        projet = new Projet();
        projet.setNom("Projet IDOR Test");
        projet.setClient(client);
        projet = projetRepository.save(projet);

        affecter(employeA);
        affecter(employeB);
    }

    private Manager creerManager(String email) {
        Manager m = new Manager();
        m.setNom("Chef");
        m.setPrenom(email.substring(0, email.indexOf('.')));
        m.setEmail(email);
        m.setMotDePasse(passwordEncoder.encode("Password123!"));
        return managerRepository.save(m);
    }

    private Employe creerEmploye(String email, Manager manager) {
        Employe e = new Employe();
        e.setNom("Agent");
        e.setPrenom(email.substring(0, email.indexOf('.')));
        e.setEmail(email);
        e.setMotDePasse(passwordEncoder.encode("Password123!"));
        e.setManager(manager);
        return employeRepository.save(e);
    }

    private void affecter(Employe employe) {
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

    private Imputation imputationEnAttente(Employe employe, LocalDate date) {
        Imputation i = new Imputation();
        i.setNom("Développement");
        i.setStatut(StatutImputation.EN_ATTENTE);
        i.setEmploye(employe);
        i.setProjet(projet);
        i.setDateImputation(date);
        i.setHeures(5.0);
        return imputationRepository.save(i);
    }

    private Absence absenceEnAttente(Employe employe) {
        Absence a = new Absence();
        a.setCollaborateur(employe);
        a.setTypeAbsence(TypeAbsence.SANS_SOLDE);
        a.setDateDebut(LocalDate.now().plusDays(10));
        a.setDateFin(LocalDate.now().plusDays(12));
        a.setNombreJours(3.0);
        a.setStatut(StatutAbsence.EN_ATTENTE);
        return absenceRepository.save(a);
    }

    private static String imputationBody(Long projetId, LocalDate date) {
        return "{\"projetId\":%d,\"dateImputation\":\"%s\",\"heures\":4.0,\"nom\":\"Tâche\"}"
                .formatted(projetId, date);
    }

    private static String absenceBody() {
        return "{\"typeAbsence\":\"SANS_SOLDE\",\"dateDebut\":\"%s\",\"dateFin\":\"%s\",\"commentaireEmploye\":\"RAS\"}"
                .formatted(LocalDate.now().plusDays(30), LocalDate.now().plusDays(31));
    }

    // =================================================================================
    // Imputations : l'employé A ne peut pas agir au nom de l'employé B
    // =================================================================================

    @Nested
    @DisplayName("Imputations — usurpation d'un autre employé")
    class ImputationsEmploye {

        @Test
        @DisplayName("POST /employe/{id} : créer au nom d'un autre employé -> 403")
        void creer_pourAutrui_forbidden() throws Exception {
            mockMvc.perform(post("/api/imputations/employe/" + employeB.getId())
                            .header("Authorization", tokenFor(employeA.getEmail()))
                            .contentType("application/json")
                            .content(imputationBody(projet.getId(), LocalDate.now())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /employe/{id} : créer en son propre nom -> 201 (contrôle positif)")
        void creer_pourSoi_created() throws Exception {
            mockMvc.perform(post("/api/imputations/employe/" + employeA.getId())
                            .header("Authorization", tokenFor(employeA.getEmail()))
                            .contentType("application/json")
                            .content(imputationBody(projet.getId(), LocalDate.now())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.employeId").value(employeA.getId()));
        }

        @Test
        @DisplayName("PUT /{id} : modifier l'imputation d'un autre employé -> 403")
        void modifier_pourAutrui_forbidden() throws Exception {
            Imputation deB = imputationEnAttente(employeB, LocalDate.now());

            mockMvc.perform(put("/api/imputations/" + deB.getId())
                            .param("employeId", String.valueOf(employeB.getId()))
                            .header("Authorization", tokenFor(employeA.getEmail()))
                            .contentType("application/json")
                            .content(imputationBody(projet.getId(), LocalDate.now())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PUT /{id} : l'imputation de B reste inaccessible même sans paramètre employeId -> 403")
        void modifier_sansParametre_resteCloisonne() throws Exception {
            Imputation deB = imputationEnAttente(employeB, LocalDate.now());

            // Sans paramètre, l'id retenu est celui du jeton (A) : le service constate que
            // l'imputation ne lui appartient pas.
            mockMvc.perform(put("/api/imputations/" + deB.getId())
                            .header("Authorization", tokenFor(employeA.getEmail()))
                            .contentType("application/json")
                            .content(imputationBody(projet.getId(), LocalDate.now())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DELETE /{id} : supprimer l'imputation d'un autre employé -> 403")
        void supprimer_pourAutrui_forbidden() throws Exception {
            Imputation deB = imputationEnAttente(employeB, LocalDate.now());

            mockMvc.perform(delete("/api/imputations/" + deB.getId())
                            .param("employeId", String.valueOf(employeB.getId()))
                            .header("Authorization", tokenFor(employeA.getEmail())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DELETE /{id} : supprimer sa propre imputation -> 200 (contrôle positif)")
        void supprimer_laSienne_ok() throws Exception {
            Imputation deA = imputationEnAttente(employeA, LocalDate.now());

            mockMvc.perform(delete("/api/imputations/" + deA.getId())
                            .param("employeId", String.valueOf(employeA.getId()))
                            .header("Authorization", tokenFor(employeA.getEmail())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /employe/{id} : lire l'historique d'un autre employé -> 403")
        void lireHistorique_dAutrui_forbidden() throws Exception {
            mockMvc.perform(get("/api/imputations/employe/" + employeB.getId())
                            .header("Authorization", tokenFor(employeA.getEmail())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /employe/{id} : un manager conserve l'accès à l'historique d'un employé -> 200")
        void lireHistorique_parManager_ok() throws Exception {
            mockMvc.perform(get("/api/imputations/employe/" + employeB.getId())
                            .header("Authorization", tokenFor(managerA.getEmail())))
                    .andExpect(status().isOk());
        }
    }

    // =================================================================================
    // Imputations : un manager ne valide que son équipe, et sous sa propre identité
    // =================================================================================

    @Nested
    @DisplayName("Imputations — usurpation d'un autre manager")
    class ImputationsManager {

        @Test
        @DisplayName("POST /valider : annoncer l'id du manager légitime ne suffit plus -> 403")
        void valider_enUsurpantLeManagerLegitime_forbidden() throws Exception {
            Imputation deA = imputationEnAttente(employeA, LocalDate.now());

            // Le cœur de la faille : managerB envoyait managerId=managerA et la vérification
            // de légitimité, qui comparait deux valeurs fournies par le client, passait.
            mockMvc.perform(post("/api/imputations/" + deA.getId() + "/valider")
                            .param("managerId", String.valueOf(managerA.getId()))
                            .header("Authorization", tokenFor(managerB.getEmail())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /valider : manager hors équipe, sans paramètre -> 403")
        void valider_horsEquipe_forbidden() throws Exception {
            Imputation deA = imputationEnAttente(employeA, LocalDate.now());

            mockMvc.perform(post("/api/imputations/" + deA.getId() + "/valider")
                            .header("Authorization", tokenFor(managerB.getEmail())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /valider : le manager de l'employé valide, sans paramètre -> 200 (contrôle positif)")
        void valider_parSonManager_ok() throws Exception {
            Imputation deA = imputationEnAttente(employeA, LocalDate.now());

            mockMvc.perform(post("/api/imputations/" + deA.getId() + "/valider")
                            .header("Authorization", tokenFor(managerA.getEmail())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statut").value("VALIDEE"))
                    .andExpect(jsonPath("$.managerValidateurId").value(managerA.getId()));
        }

        @Test
        @DisplayName("POST /rejeter : annoncer l'id du manager légitime ne suffit plus -> 403")
        void rejeter_enUsurpantLeManagerLegitime_forbidden() throws Exception {
            Imputation deA = imputationEnAttente(employeA, LocalDate.now());

            mockMvc.perform(post("/api/imputations/" + deA.getId() + "/rejeter")
                            .param("managerId", String.valueOf(managerA.getId()))
                            .header("Authorization", tokenFor(managerB.getEmail()))
                            .contentType("application/json")
                            .content("{\"motif\":\"Tentative d'usurpation\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /en-attente : consulter la file d'un autre manager -> 403")
        void fileDAttente_dUnAutreManager_forbidden() throws Exception {
            mockMvc.perform(get("/api/imputations/en-attente")
                            .param("managerId", String.valueOf(managerA.getId()))
                            .header("Authorization", tokenFor(managerB.getEmail())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /en-attente : sans paramètre, un manager voit sa propre file -> 200 (contrôle positif)")
        void fileDAttente_sansParametre_ok() throws Exception {
            imputationEnAttente(employeA, LocalDate.now());

            mockMvc.perform(get("/api/imputations/en-attente")
                            .header("Authorization", tokenFor(managerA.getEmail())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("GET /manager/{id} : consulter les validations d'un autre manager -> 403")
        void validationsDUnAutreManager_forbidden() throws Exception {
            mockMvc.perform(get("/api/imputations/manager/" + managerA.getId())
                            .header("Authorization", tokenFor(managerB.getEmail())))
                    .andExpect(status().isForbidden());
        }
    }

    // =================================================================================
    // Absences : mêmes règles
    // =================================================================================

    @Nested
    @DisplayName("Absences — usurpation d'un autre collaborateur")
    class Absences {

        @Test
        @DisplayName("POST /employe/{id} : déposer une absence au nom d'un autre -> 403")
        void deposer_pourAutrui_forbidden() throws Exception {
            mockMvc.perform(post("/api/absences/employe/" + employeB.getId())
                            .header("Authorization", tokenFor(employeA.getEmail()))
                            .contentType("application/json")
                            .content(absenceBody()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /employe/{id} : déposer sa propre absence -> 201 (contrôle positif)")
        void deposer_pourSoi_created() throws Exception {
            mockMvc.perform(post("/api/absences/employe/" + employeA.getId())
                            .header("Authorization", tokenFor(employeA.getEmail()))
                            .contentType("application/json")
                            .content(absenceBody()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.employeId").value(employeA.getId()));
        }

        @Test
        @DisplayName("DELETE /{id}/employe/{id} : annuler l'absence d'un autre -> 403")
        void annuler_dAutrui_forbidden() throws Exception {
            Absence deB = absenceEnAttente(employeB);

            mockMvc.perform(delete("/api/absences/" + deB.getId() + "/employe/" + employeB.getId())
                            .header("Authorization", tokenFor(employeA.getEmail())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DELETE /{id}/employe/{id} : annuler sa propre absence -> 204 (contrôle positif)")
        void annuler_laSienne_noContent() throws Exception {
            Absence deA = absenceEnAttente(employeA);

            mockMvc.perform(delete("/api/absences/" + deA.getId() + "/employe/" + employeA.getId())
                            .header("Authorization", tokenFor(employeA.getEmail())))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("GET /employe/{id} : lire les absences d'un autre employé -> 403")
        void lister_dAutrui_forbidden() throws Exception {
            mockMvc.perform(get("/api/absences/employe/" + employeB.getId())
                            .header("Authorization", tokenFor(employeA.getEmail())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /quotas/employe/{id}/annee/{annee} : lire les quotas d'un autre employé -> 403")
        void quotas_dAutrui_forbidden() throws Exception {
            mockMvc.perform(get("/api/absences/quotas/employe/" + employeB.getId() + "/annee/2026")
                            .header("Authorization", tokenFor(employeA.getEmail())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /quotas/employe/{id}/annee/{annee} : lire ses propres quotas -> 200 (contrôle positif)")
        void quotas_lesSiens_ok() throws Exception {
            mockMvc.perform(get("/api/absences/quotas/employe/" + employeA.getId() + "/annee/2026")
                            .header("Authorization", tokenFor(employeA.getEmail())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /{id}/justificatif : déposer un justificatif sur l'absence d'un autre -> 403")
        void justificatif_surAbsenceDAutrui_forbidden() throws Exception {
            Absence deB = absenceEnAttente(employeB);
            MockMultipartFile fichier = new MockMultipartFile(
                    "fichier", "arret.pdf", "application/pdf", "contenu".getBytes());

            mockMvc.perform(multipart("/api/absences/" + deB.getId() + "/justificatif")
                            .file(fichier)
                            .header("Authorization", tokenFor(employeA.getEmail())))
                    .andExpect(status().isForbidden());
        }
    }

    // =================================================================================
    // Absences : périmètre hiérarchique du valideur
    // =================================================================================

    @Nested
    @DisplayName("Absences — périmètre du valideur")
    class ValidationAbsences {

        @Test
        @DisplayName("POST /valider : annoncer l'id du manager légitime ne suffit plus -> 403")
        void valider_enUsurpantLeManagerLegitime_forbidden() throws Exception {
            Absence deA = absenceEnAttente(employeA);

            mockMvc.perform(post("/api/absences/" + deA.getId() + "/valider")
                            .param("managerId", String.valueOf(managerA.getId()))
                            .header("Authorization", tokenFor(managerB.getEmail())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /valider : manager hors équipe, sans paramètre -> 403")
        void valider_horsEquipe_forbidden() throws Exception {
            Absence deA = absenceEnAttente(employeA);

            mockMvc.perform(post("/api/absences/" + deA.getId() + "/valider")
                            .header("Authorization", tokenFor(managerB.getEmail())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /valider : le manager de l'employé valide -> 200 (contrôle positif)")
        void valider_parSonManager_ok() throws Exception {
            Absence deA = absenceEnAttente(employeA);

            mockMvc.perform(post("/api/absences/" + deA.getId() + "/valider")
                            .header("Authorization", tokenFor(managerA.getEmail())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statut").value("VALIDEE"))
                    .andExpect(jsonPath("$.managerValidateurId").value(managerA.getId()));
        }

        @Test
        @DisplayName("POST /rejeter : manager hors équipe -> 403")
        void rejeter_horsEquipe_forbidden() throws Exception {
            Absence deA = absenceEnAttente(employeA);

            mockMvc.perform(post("/api/absences/" + deA.getId() + "/rejeter")
                            .param("managerId", String.valueOf(managerA.getId()))
                            .header("Authorization", tokenFor(managerB.getEmail()))
                            .contentType("application/json")
                            .content("{\"motif\":\"Tentative d'usurpation\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    // =================================================================================
    // Affectations : lectures ouvertes aux employés
    // =================================================================================

    @Nested
    @DisplayName("Affectations — lecture du dossier d'un autre collaborateur")
    class Affectations {

        @Test
        @DisplayName("GET /collaborateur/{id} : lire les affectations d'un autre -> 403")
        void affectations_dAutrui_forbidden() throws Exception {
            mockMvc.perform(get("/api/affectations/collaborateur/" + employeB.getId())
                            .header("Authorization", tokenFor(employeA.getEmail())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /collaborateur/{id} : lire les siennes -> 200 (contrôle positif)")
        void affectations_lesSiennes_ok() throws Exception {
            mockMvc.perform(get("/api/affectations/collaborateur/" + employeA.getId())
                            .header("Authorization", tokenFor(employeA.getEmail())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("GET /{collaborateurId}/{projetId} : lire l'affectation d'un autre -> 403")
        void affectation_dAutrui_forbidden() throws Exception {
            mockMvc.perform(get("/api/affectations/" + employeB.getId() + "/" + projet.getId())
                            .header("Authorization", tokenFor(employeA.getEmail())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /collaborateur/{id}/taux-total : lire le taux d'un autre -> 403")
        void tauxTotal_dAutrui_forbidden() throws Exception {
            mockMvc.perform(get("/api/affectations/collaborateur/" + employeB.getId() + "/taux-total")
                            .header("Authorization", tokenFor(employeA.getEmail())))
                    .andExpect(status().isForbidden());
        }
    }

    // =================================================================================
    // Compte : changement de mot de passe
    // =================================================================================

    @Nested
    @DisplayName("Compte — changement de mot de passe")
    class ChangementMotDePasse {

        @Test
        @DisplayName("POST /{id}/change-password : cibler le compte d'un autre -> 403")
        void changerLeMotDePasseDAutrui_forbidden() throws Exception {
            mockMvc.perform(post("/api/collaborateurs/" + employeB.getId() + "/change-password")
                            .header("Authorization", tokenFor(employeA.getEmail()))
                            .contentType("application/json")
                            .content("{\"ancienMotDePasse\":\"Password123!\",\"nouveauMotDePasse\":\"Nouveau123!\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /{id}/change-password : changer le sien -> 200 (contrôle positif)")
        void changerSonPropreMotDePasse_ok() throws Exception {
            mockMvc.perform(post("/api/collaborateurs/" + employeA.getId() + "/change-password")
                            .header("Authorization", tokenFor(employeA.getEmail()))
                            .contentType("application/json")
                            .content("{\"ancienMotDePasse\":\"Password123!\",\"nouveauMotDePasse\":\"Nouveau123!\"}"))
                    .andExpect(status().isOk());
        }
    }
}
