package com.SSS.SGI.service;

import com.SSS.SGI.dto.CreateImputationRequest;
import com.SSS.SGI.entity.Affectation;
import com.SSS.SGI.entity.Employe;
import com.SSS.SGI.entity.Imputation;
import com.SSS.SGI.entity.Manager;
import com.SSS.SGI.entity.Projet;
import com.SSS.SGI.entity.StatutImputation;
import com.SSS.SGI.exception.ImputationNonAutoriseeException;
import com.SSS.SGI.exception.ManagerNonAutoriseException;
import com.SSS.SGI.exception.ResourceNotFoundException;
import com.SSS.SGI.exception.ValidationException;
import com.SSS.SGI.repository.AffectationRepository;
import com.SSS.SGI.repository.EmployeRepository;
import com.SSS.SGI.repository.ImputationRepository;
import com.SSS.SGI.repository.ManagerRepository;
import com.SSS.SGI.repository.ProjetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImputationServiceTest {

    @Mock
    private ImputationRepository imputationRepository;
    @Mock
    private EmployeRepository employeRepository;
    @Mock
    private ManagerRepository managerRepository;
    @Mock
    private ProjetRepository projetRepository;
    @Mock
    private AffectationRepository affectationRepository;

    private ImputationService imputationService;

    private static final Long EMPLOYE_ID = 1L;
    private static final Long PROJET_ID = 10L;
    private static final Long MANAGER_ID = 20L;
    private static final Long AUTRE_MANAGER_ID = 21L;
    private static final Long IMPUTATION_ID = 100L;

    private Employe employe;
    private Projet projet;
    private Manager manager;

    @BeforeEach
    void setUp() {
        imputationService = new ImputationService(
                imputationRepository, employeRepository, managerRepository, projetRepository, affectationRepository);

        manager = new Manager();
        manager.setId(MANAGER_ID);
        manager.setNom("Smith");
        manager.setPrenom("John");

        employe = new Employe();
        employe.setId(EMPLOYE_ID);
        employe.setNom("Doe");
        employe.setPrenom("Jane");
        employe.setManager(manager);

        projet = new Projet();
        projet.setId(PROJET_ID);
        projet.setNom("Migration cloud");

        // Par défaut, aucune heure déjà imputée ce jour-là ; les tests du plafond journalier
        // surchargent ce stub avec une valeur explicite.
        lenient().when(imputationRepository.sumHeuresDuJour(any(), any(), any())).thenReturn(0.0);
    }

    private CreateImputationRequest requestValide() {
        return new CreateImputationRequest(PROJET_ID, LocalDate.now(), 6.0, "Développement");
    }

    @Nested
    @DisplayName("creerImputation")
    class CreerImputation {

        @Test
        @DisplayName("Lève ResourceNotFoundException si l'employé n'existe pas")
        void employeIntrouvable_lanceException() {
            when(employeRepository.findById(EMPLOYE_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> imputationService.creerImputation(EMPLOYE_ID, requestValide()));

            verifyNoInteractions(imputationRepository, affectationRepository);
        }

        @Test
        @DisplayName("Lève ResourceNotFoundException si le projet n'existe pas")
        void projetIntrouvable_lanceException() {
            when(employeRepository.findById(EMPLOYE_ID)).thenReturn(Optional.of(employe));
            when(projetRepository.findById(PROJET_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> imputationService.creerImputation(EMPLOYE_ID, requestValide()));

            verifyNoInteractions(affectationRepository);
        }

        @Test
        @DisplayName("Lève ValidationException si les heures sont à zéro")
        void heuresAZero_lanceException() {
            when(employeRepository.findById(EMPLOYE_ID)).thenReturn(Optional.of(employe));
            when(projetRepository.findById(PROJET_ID)).thenReturn(Optional.of(projet));

            CreateImputationRequest request = new CreateImputationRequest(PROJET_ID, LocalDate.now(), 0.0, "Dev");

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> imputationService.creerImputation(EMPLOYE_ID, request));
            assertTrue(ex.getMessage().contains("supérieur à 0"));
            verify(imputationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lève ValidationException si les heures dépassent 24")
        void heuresSuperieuresA24_lanceException() {
            when(employeRepository.findById(EMPLOYE_ID)).thenReturn(Optional.of(employe));
            when(projetRepository.findById(PROJET_ID)).thenReturn(Optional.of(projet));

            CreateImputationRequest request = new CreateImputationRequest(PROJET_ID, LocalDate.now(), 24.5, "Dev");

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> imputationService.creerImputation(EMPLOYE_ID, request));
            assertTrue(ex.getMessage().contains("24"));
            verify(imputationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lève ValidationException si la date dépasse un an dans le futur")
        void dateTropLointaine_lanceException() {
            when(employeRepository.findById(EMPLOYE_ID)).thenReturn(Optional.of(employe));
            when(projetRepository.findById(PROJET_ID)).thenReturn(Optional.of(projet));

            CreateImputationRequest request = new CreateImputationRequest(
                    PROJET_ID, LocalDate.now().plusYears(1).plusDays(1), 4.0, "Dev");

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> imputationService.creerImputation(EMPLOYE_ID, request));
            assertTrue(ex.getMessage().contains("un an"));
            verify(imputationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lève ValidationException si le cumul du jour dépasse 8h (5h existantes + 4h nouvelles)")
        void plafondJournalierDepasse_lanceException() {
            when(employeRepository.findById(EMPLOYE_ID)).thenReturn(Optional.of(employe));
            when(projetRepository.findById(PROJET_ID)).thenReturn(Optional.of(projet));

            CreateImputationRequest request = new CreateImputationRequest(PROJET_ID, LocalDate.now(), 4.0, "Dev");
            when(imputationRepository.sumHeuresDuJour(EMPLOYE_ID, request.dateImputation(), null)).thenReturn(5.0);

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> imputationService.creerImputation(EMPLOYE_ID, request));
            assertTrue(ex.getMessage().contains("Plafond journalier"));
            assertTrue(ex.getMessage().contains("8h"));
            verify(imputationRepository, never()).save(any());
            // La règle du plafond se déclenche avant le contrôle d'affectation : celui-ci ne doit
            // jamais être atteint pour cette requête.
            verifyNoInteractions(affectationRepository);
        }

        @Test
        @DisplayName("Accepte l'imputation quand le cumul du jour atteint exactement 8h")
        void plafondJournalierAtteintExactement_succes() {
            when(employeRepository.findById(EMPLOYE_ID)).thenReturn(Optional.of(employe));
            when(projetRepository.findById(PROJET_ID)).thenReturn(Optional.of(projet));
            when(affectationRepository.findByCollaborateurIdAndProjetId(EMPLOYE_ID, PROJET_ID))
                    .thenReturn(Optional.of(mock(Affectation.class)));

            CreateImputationRequest request = new CreateImputationRequest(PROJET_ID, LocalDate.now(), 3.0, "Dev");
            when(imputationRepository.sumHeuresDuJour(EMPLOYE_ID, request.dateImputation(), null)).thenReturn(5.0);
            when(imputationRepository.findDoublon(EMPLOYE_ID, PROJET_ID, request.dateImputation(), null))
                    .thenReturn(Optional.empty());
            when(imputationRepository.save(any(Imputation.class))).thenAnswer(inv -> inv.getArgument(0));

            imputationService.creerImputation(EMPLOYE_ID, request);

            verify(imputationRepository).save(any(Imputation.class));
        }

        @Test
        @DisplayName("Lève ValidationException si l'employé n'est pas affecté au projet")
        void employeNonAffecte_lanceException() {
            when(employeRepository.findById(EMPLOYE_ID)).thenReturn(Optional.of(employe));
            when(projetRepository.findById(PROJET_ID)).thenReturn(Optional.of(projet));
            when(affectationRepository.findByCollaborateurIdAndProjetId(EMPLOYE_ID, PROJET_ID))
                    .thenReturn(Optional.empty());

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> imputationService.creerImputation(EMPLOYE_ID, requestValide()));
            assertTrue(ex.getMessage().contains("affectation"));
            verify(imputationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lève ValidationException en cas de doublon employé+projet+date")
        void doublonExistant_lanceException() {
            when(employeRepository.findById(EMPLOYE_ID)).thenReturn(Optional.of(employe));
            when(projetRepository.findById(PROJET_ID)).thenReturn(Optional.of(projet));
            when(affectationRepository.findByCollaborateurIdAndProjetId(EMPLOYE_ID, PROJET_ID))
                    .thenReturn(Optional.of(mock(Affectation.class)));

            CreateImputationRequest request = requestValide();
            when(imputationRepository.findDoublon(EMPLOYE_ID, PROJET_ID, request.dateImputation(), null))
                    .thenReturn(Optional.of(mock(Imputation.class)));

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> imputationService.creerImputation(EMPLOYE_ID, request));
            assertTrue(ex.getMessage().contains("existe déjà"));
            verify(imputationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Crée l'imputation en EN_ATTENTE quand toutes les règles sont respectées")
        void reglesRespectees_succes() {
            when(employeRepository.findById(EMPLOYE_ID)).thenReturn(Optional.of(employe));
            when(projetRepository.findById(PROJET_ID)).thenReturn(Optional.of(projet));
            when(affectationRepository.findByCollaborateurIdAndProjetId(EMPLOYE_ID, PROJET_ID))
                    .thenReturn(Optional.of(mock(Affectation.class)));

            CreateImputationRequest request = requestValide();
            when(imputationRepository.findDoublon(EMPLOYE_ID, PROJET_ID, request.dateImputation(), null))
                    .thenReturn(Optional.empty());
            when(imputationRepository.save(any(Imputation.class))).thenAnswer(inv -> inv.getArgument(0));

            imputationService.creerImputation(EMPLOYE_ID, request);

            ArgumentCaptor<Imputation> captor = ArgumentCaptor.forClass(Imputation.class);
            verify(imputationRepository).save(captor.capture());
            assertEquals(StatutImputation.EN_ATTENTE, captor.getValue().getStatut());
            assertEquals(6.0, captor.getValue().getHeures());
            assertEquals(request.dateImputation(), captor.getValue().getDateImputation());
        }
    }

    @Nested
    @DisplayName("updateImputation")
    class UpdateImputation {

        private Imputation existante() {
            Imputation i = new Imputation();
            i.setId(IMPUTATION_ID);
            i.setStatut(StatutImputation.EN_ATTENTE);
            i.setEmploye(employe);
            i.setProjet(projet);
            i.setNom("Ancien nom");
            i.setDateImputation(LocalDate.now().minusDays(1));
            i.setHeures(3.0);
            return i;
        }

        @Test
        @DisplayName("Lève ImputationNonAutoriseeException si ce n'est pas l'employé propriétaire")
        void pasProprietaire_lanceException() {
            when(imputationRepository.findById(IMPUTATION_ID)).thenReturn(Optional.of(existante()));

            assertThrows(ImputationNonAutoriseeException.class,
                    () -> imputationService.updateImputation(IMPUTATION_ID, 999L, requestValide()));
        }

        @Test
        @DisplayName("Lève IllegalStateException si l'imputation n'est plus EN_ATTENTE")
        void pasEnAttente_lanceException() {
            Imputation i = existante();
            i.setStatut(StatutImputation.VALIDEE);
            when(imputationRepository.findById(IMPUTATION_ID)).thenReturn(Optional.of(i));

            assertThrows(IllegalStateException.class,
                    () -> imputationService.updateImputation(IMPUTATION_ID, EMPLOYE_ID, requestValide()));
        }

        @Test
        @DisplayName("Le contrôle anti-doublon exclut l'imputation elle-même")
        void doublonExclutImputationCourante_succes() {
            Imputation i = existante();
            when(imputationRepository.findById(IMPUTATION_ID)).thenReturn(Optional.of(i));
            when(projetRepository.findById(PROJET_ID)).thenReturn(Optional.of(projet));
            when(affectationRepository.findByCollaborateurIdAndProjetId(EMPLOYE_ID, PROJET_ID))
                    .thenReturn(Optional.of(mock(Affectation.class)));

            CreateImputationRequest request = requestValide();
            when(imputationRepository.findDoublon(EMPLOYE_ID, PROJET_ID, request.dateImputation(), IMPUTATION_ID))
                    .thenReturn(Optional.empty());
            when(imputationRepository.save(any(Imputation.class))).thenAnswer(inv -> inv.getArgument(0));

            imputationService.updateImputation(IMPUTATION_ID, EMPLOYE_ID, request);

            verify(imputationRepository).findDoublon(EMPLOYE_ID, PROJET_ID, request.dateImputation(), IMPUTATION_ID);
            verify(imputationRepository).save(any(Imputation.class));
        }
    }

    @Nested
    @DisplayName("deleteImputation")
    class DeleteImputation {

        private Imputation existante(StatutImputation statut) {
            Imputation i = new Imputation();
            i.setId(IMPUTATION_ID);
            i.setStatut(statut);
            i.setEmploye(employe);
            return i;
        }

        @Test
        @DisplayName("Lève ImputationNonAutoriseeException si ce n'est pas l'employé propriétaire")
        void pasProprietaire_lanceException() {
            when(imputationRepository.findById(IMPUTATION_ID)).thenReturn(Optional.of(existante(StatutImputation.EN_ATTENTE)));

            assertThrows(ImputationNonAutoriseeException.class,
                    () -> imputationService.deleteImputation(IMPUTATION_ID, 999L));
            verify(imputationRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Lève IllegalStateException si l'imputation n'est plus EN_ATTENTE")
        void pasEnAttente_lanceException() {
            when(imputationRepository.findById(IMPUTATION_ID)).thenReturn(Optional.of(existante(StatutImputation.VALIDEE)));

            assertThrows(IllegalStateException.class,
                    () -> imputationService.deleteImputation(IMPUTATION_ID, EMPLOYE_ID));
            verify(imputationRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Supprime l'imputation quand l'employé est propriétaire et qu'elle est EN_ATTENTE")
        void proprietaireEtEnAttente_succes() {
            when(imputationRepository.findById(IMPUTATION_ID)).thenReturn(Optional.of(existante(StatutImputation.EN_ATTENTE)));

            imputationService.deleteImputation(IMPUTATION_ID, EMPLOYE_ID);

            verify(imputationRepository).deleteById(IMPUTATION_ID);
        }
    }

    @Nested
    @DisplayName("validerImputation")
    class ValiderImputation {

        private Imputation existante() {
            Imputation i = new Imputation();
            i.setId(IMPUTATION_ID);
            i.setStatut(StatutImputation.EN_ATTENTE);
            i.setEmploye(employe);
            i.setProjet(projet);
            return i;
        }

        @Test
        @DisplayName("Lève ResourceNotFoundException si l'imputation n'existe pas")
        void imputationIntrouvable_lanceException() {
            when(imputationRepository.findById(IMPUTATION_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> imputationService.validerImputation(IMPUTATION_ID, MANAGER_ID));
        }

        @Test
        @DisplayName("Lève ResourceNotFoundException si le manager n'existe pas")
        void managerIntrouvable_lanceException() {
            when(imputationRepository.findById(IMPUTATION_ID)).thenReturn(Optional.of(existante()));
            when(managerRepository.findById(MANAGER_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> imputationService.validerImputation(IMPUTATION_ID, MANAGER_ID));
        }

        @Test
        @DisplayName("Lève ManagerNonAutoriseException si le manager n'est pas celui de l'employé")
        void managerNonLegitime_lanceException() {
            when(imputationRepository.findById(IMPUTATION_ID)).thenReturn(Optional.of(existante()));
            when(managerRepository.findById(AUTRE_MANAGER_ID)).thenReturn(Optional.of(new Manager()));

            assertThrows(ManagerNonAutoriseException.class,
                    () -> imputationService.validerImputation(IMPUTATION_ID, AUTRE_MANAGER_ID));
            verify(imputationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Valide l'imputation quand le manager est légitime")
        void managerLegitime_succes() {
            when(imputationRepository.findById(IMPUTATION_ID)).thenReturn(Optional.of(existante()));
            when(managerRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
            when(imputationRepository.save(any(Imputation.class))).thenAnswer(inv -> inv.getArgument(0));

            imputationService.validerImputation(IMPUTATION_ID, MANAGER_ID);

            ArgumentCaptor<Imputation> captor = ArgumentCaptor.forClass(Imputation.class);
            verify(imputationRepository).save(captor.capture());
            assertEquals(StatutImputation.VALIDEE, captor.getValue().getStatut());
        }

        @Test
        @DisplayName("Lève IllegalStateException si l'imputation n'est pas EN_ATTENTE")
        void dejaTraitee_lanceException() {
            Imputation i = existante();
            i.setStatut(StatutImputation.REJETEE);
            when(imputationRepository.findById(IMPUTATION_ID)).thenReturn(Optional.of(i));
            when(managerRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));

            assertThrows(IllegalStateException.class,
                    () -> imputationService.validerImputation(IMPUTATION_ID, MANAGER_ID));
        }
    }

    @Nested
    @DisplayName("rejeterImputation")
    class RejeterImputation {

        private Imputation existante() {
            Imputation i = new Imputation();
            i.setId(IMPUTATION_ID);
            i.setStatut(StatutImputation.EN_ATTENTE);
            i.setEmploye(employe);
            i.setProjet(projet);
            return i;
        }

        @Test
        @DisplayName("Lève ManagerNonAutoriseException si le manager n'est pas celui de l'employé")
        void managerNonLegitime_lanceException() {
            when(imputationRepository.findById(IMPUTATION_ID)).thenReturn(Optional.of(existante()));
            when(managerRepository.findById(AUTRE_MANAGER_ID)).thenReturn(Optional.of(new Manager()));

            assertThrows(ManagerNonAutoriseException.class,
                    () -> imputationService.rejeterImputation(IMPUTATION_ID, AUTRE_MANAGER_ID, "Motif"));
        }

        @Test
        @DisplayName("Rejette l'imputation et enregistre le motif")
        void managerLegitime_succesAvecMotif() {
            when(imputationRepository.findById(IMPUTATION_ID)).thenReturn(Optional.of(existante()));
            when(managerRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
            when(imputationRepository.save(any(Imputation.class))).thenAnswer(inv -> inv.getArgument(0));

            imputationService.rejeterImputation(IMPUTATION_ID, MANAGER_ID, "Description trop vague");

            ArgumentCaptor<Imputation> captor = ArgumentCaptor.forClass(Imputation.class);
            verify(imputationRepository).save(captor.capture());
            assertEquals(StatutImputation.REJETEE, captor.getValue().getStatut());
            assertEquals("Description trop vague", captor.getValue().getMotifRejet());
        }
    }

    @Nested
    @DisplayName("getImputationsEnAttenteForManager")
    class GetImputationsEnAttenteForManager {

        @Test
        @DisplayName("Ne retourne que les imputations des employés de ce manager")
        void filtreParManager() {
            Manager autreManager = new Manager();
            autreManager.setId(AUTRE_MANAGER_ID);

            Employe autreEmploye = new Employe();
            autreEmploye.setId(2L);
            autreEmploye.setManager(autreManager);

            Imputation deMonEquipe = new Imputation();
            deMonEquipe.setId(1L);
            deMonEquipe.setStatut(StatutImputation.EN_ATTENTE);
            deMonEquipe.setEmploye(employe);
            deMonEquipe.setProjet(projet);
            deMonEquipe.setNom("A");

            Imputation dAutreEquipe = new Imputation();
            dAutreEquipe.setId(2L);
            dAutreEquipe.setStatut(StatutImputation.EN_ATTENTE);
            dAutreEquipe.setEmploye(autreEmploye);
            dAutreEquipe.setProjet(projet);
            dAutreEquipe.setNom("B");

            when(imputationRepository.findByStatut(StatutImputation.EN_ATTENTE))
                    .thenReturn(List.of(deMonEquipe, dAutreEquipe));

            List<?> resultat = imputationService.getImputationsEnAttenteForManager(MANAGER_ID);

            assertEquals(1, resultat.size());
        }
    }

    @Nested
    @DisplayName("getCumulHeuresValideesByProjet")
    class GetCumulHeures {

        @Test
        @DisplayName("Lève ResourceNotFoundException si le projet n'existe pas")
        void projetIntrouvable_lanceException() {
            when(projetRepository.findById(PROJET_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> imputationService.getCumulHeuresValideesByProjet(PROJET_ID));
        }

        @Test
        @DisplayName("Retourne le total d'heures validées renvoyé par le dépôt")
        void projetExistant_retourneLeTotal() {
            when(projetRepository.findById(PROJET_ID)).thenReturn(Optional.of(projet));
            when(imputationRepository.sumHeuresValideesByProjet(PROJET_ID)).thenReturn(42.5);

            Double total = imputationService.getCumulHeuresValideesByProjet(PROJET_ID);

            assertEquals(42.5, total);
        }
    }
}
