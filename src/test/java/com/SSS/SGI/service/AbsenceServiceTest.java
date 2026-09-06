package com.SSS.SGI.service;

import com.SSS.SGI.dto.AbsenceDTO;
import com.SSS.SGI.dto.CreateAbsenceRequest;
import com.SSS.SGI.entity.Absence;
import com.SSS.SGI.entity.Employe;
import com.SSS.SGI.entity.Manager;
import com.SSS.SGI.entity.QuotaAbsence;
import com.SSS.SGI.entity.enums.StatutAbsence;
import com.SSS.SGI.entity.enums.TypeAbsence;
import com.SSS.SGI.exception.AbsenceChevauchementException;
import com.SSS.SGI.exception.AdminNonAutoriseException;
import com.SSS.SGI.exception.ManagerNonAutoriseException;
import com.SSS.SGI.exception.QuotaInsuffisantException;
import com.SSS.SGI.exception.ResourceNotFoundException;
import com.SSS.SGI.repository.AbsenceRepository;
import com.SSS.SGI.repository.CollaborateurRepository;
import com.SSS.SGI.repository.ManagerRepository;
import com.SSS.SGI.repository.QuotaAbsenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbsenceServiceTest {

    @Mock
    private AbsenceRepository absenceRepository;
    @Mock
    private QuotaAbsenceRepository quotaAbsenceRepository;
    @Mock
    private CollaborateurRepository collaborateurRepository;
    @Mock
    private ManagerRepository managerRepository;

    private AbsenceService absenceService;

    private Employe employeMock;
    private static final Long EMPLOYE_ID = 1L;

    @BeforeEach
    void setUp() {
        absenceService = new AbsenceService(
                absenceRepository, quotaAbsenceRepository, collaborateurRepository, managerRepository);
        ReflectionTestUtils.setField(absenceService, "dossierJustificatifs", "./justificatifs-test");

        employeMock = mock(Employe.class);
        lenient().when(employeMock.getId()).thenReturn(EMPLOYE_ID);
        lenient().when(employeMock.getNom()).thenReturn("Doe");
        lenient().when(employeMock.getPrenom()).thenReturn("Jane");
    }

    /**
     * Rattache l'employé de test au manager qui va valider. Depuis le durcissement de l'identité,
     * un manager ne traite que les demandes de ses propres employés : sans ce rattachement, toute
     * validation est refusée, ce qui est précisément le comportement attendu.
     */
    private void rattacherEmployeAuManager(Long managerId) {
        Manager hierarchique = mock(Manager.class);
        lenient().when(hierarchique.getId()).thenReturn(managerId);
        when(employeMock.getManager()).thenReturn(hierarchique);
    }

    @Nested
    @DisplayName("creerAbsence")
    class CreerAbsence {

        @Test
        @DisplayName("Lève ResourceNotFoundException si le collaborateur n'existe pas")
        void collaborateurIntrouvable_lanceException() {
            when(collaborateurRepository.findById(EMPLOYE_ID)).thenReturn(Optional.empty());

            CreateAbsenceRequest request = new CreateAbsenceRequest(
                    TypeAbsence.CONGE_PAYE, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), null);

            assertThrows(ResourceNotFoundException.class,
                    () -> absenceService.creerAbsence(EMPLOYE_ID, request, false));

            verifyNoInteractions(absenceRepository, quotaAbsenceRepository);
        }

        @Test
        @DisplayName("Lève IllegalArgumentException si la date de fin précède la date de début")
        void dateFinAvantDateDebut_lanceException() {
            when(collaborateurRepository.findById(EMPLOYE_ID)).thenReturn(Optional.of(employeMock));

            CreateAbsenceRequest request = new CreateAbsenceRequest(
                    TypeAbsence.CONGE_PAYE, LocalDate.now().plusDays(5), LocalDate.now().plusDays(1), null);

            assertThrows(IllegalArgumentException.class,
                    () -> absenceService.creerAbsence(EMPLOYE_ID, request, false));
        }

        @Test
        @DisplayName("Lève IllegalArgumentException si la date de début est dans le passé")
        void dateDansLePasse_lanceException() {
            when(collaborateurRepository.findById(EMPLOYE_ID)).thenReturn(Optional.of(employeMock));

            CreateAbsenceRequest request = new CreateAbsenceRequest(
                    TypeAbsence.CONGE_PAYE, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), null);

            assertThrows(IllegalArgumentException.class,
                    () -> absenceService.creerAbsence(EMPLOYE_ID, request, false));
        }

        @Test
        @DisplayName("Lève IllegalArgumentException si la durée dépasse 90 jours")
        void dureeSuperieureA90Jours_lanceException() {
            when(collaborateurRepository.findById(EMPLOYE_ID)).thenReturn(Optional.of(employeMock));

            CreateAbsenceRequest request = new CreateAbsenceRequest(
                    TypeAbsence.CONGE_PAYE, LocalDate.now().plusDays(1), LocalDate.now().plusDays(100), null);

            assertThrows(IllegalArgumentException.class,
                    () -> absenceService.creerAbsence(EMPLOYE_ID, request, false));
        }

        @Test
        @DisplayName("Lève AbsenceChevauchementException si une absence existe déjà sur la période")
        void chevauchementExistant_lanceException() {
            when(collaborateurRepository.findById(EMPLOYE_ID)).thenReturn(Optional.of(employeMock));

            LocalDate debut = LocalDate.now().plusDays(1);
            LocalDate fin = LocalDate.now().plusDays(3);

            when(absenceRepository.findChevauchements(EMPLOYE_ID, debut, fin))
                    .thenReturn(List.of(mock(Absence.class)));

            CreateAbsenceRequest request = new CreateAbsenceRequest(TypeAbsence.CONGE_PAYE, debut, fin, null);

            assertThrows(AbsenceChevauchementException.class,
                    () -> absenceService.creerAbsence(EMPLOYE_ID, request, false));

            verifyNoInteractions(quotaAbsenceRepository);
        }

        @Test
        @DisplayName("Lève QuotaInsuffisantException si aucun quota n'est défini pour un type soumis à quota")
        void typeSoumisAQuota_quotaInexistant_lanceException() {
            when(collaborateurRepository.findById(EMPLOYE_ID)).thenReturn(Optional.of(employeMock));

            LocalDate debut = LocalDate.now().plusDays(1);
            LocalDate fin = LocalDate.now().plusDays(2);

            when(absenceRepository.findChevauchements(EMPLOYE_ID, debut, fin))
                    .thenReturn(Collections.emptyList());

            when(quotaAbsenceRepository.findByCollaborateur_IdAndTypeAbsenceAndAnnee(
                    EMPLOYE_ID, TypeAbsence.CONGE_PAYE, debut.getYear()))
                    .thenReturn(Optional.empty());

            CreateAbsenceRequest request = new CreateAbsenceRequest(TypeAbsence.CONGE_PAYE, debut, fin, null);

            assertThrows(QuotaInsuffisantException.class,
                    () -> absenceService.creerAbsence(EMPLOYE_ID, request, false));

            verify(absenceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lève QuotaInsuffisantException si le solde de quota est insuffisant")
        void typeSoumisAQuota_quotaInsuffisant_lanceException() {
            when(collaborateurRepository.findById(EMPLOYE_ID)).thenReturn(Optional.of(employeMock));

            LocalDate debut = LocalDate.now().plusDays(1);
            LocalDate fin = debut.plusDays(4);

            when(absenceRepository.findChevauchements(eq(EMPLOYE_ID), eq(debut), eq(fin)))
                    .thenReturn(Collections.emptyList());

            QuotaAbsence quota = mock(QuotaAbsence.class);
            when(quota.getJoursRestants()).thenReturn(1.0);

            when(quotaAbsenceRepository.findByCollaborateur_IdAndTypeAbsenceAndAnnee(
                    EMPLOYE_ID, TypeAbsence.CONGE_PAYE, debut.getYear()))
                    .thenReturn(Optional.of(quota));

            CreateAbsenceRequest request = new CreateAbsenceRequest(TypeAbsence.CONGE_PAYE, debut, fin, null);

            assertThrows(QuotaInsuffisantException.class,
                    () -> absenceService.creerAbsence(EMPLOYE_ID, request, false));

            verify(absenceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Crée l'absence avec succès quand le quota est suffisant")
        void typeSoumisAQuota_quotaSuffisant_succes() {
            when(collaborateurRepository.findById(EMPLOYE_ID)).thenReturn(Optional.of(employeMock));

            LocalDate debut = LocalDate.now().plusDays(1);
            LocalDate fin = debut.plusDays(1);

            when(absenceRepository.findChevauchements(eq(EMPLOYE_ID), eq(debut), eq(fin)))
                    .thenReturn(Collections.emptyList());

            QuotaAbsence quota = mock(QuotaAbsence.class);
            when(quota.getJoursRestants()).thenReturn(10.0);

            when(quotaAbsenceRepository.findByCollaborateur_IdAndTypeAbsenceAndAnnee(
                    EMPLOYE_ID, TypeAbsence.CONGE_PAYE, debut.getYear()))
                    .thenReturn(Optional.of(quota));

            Absence savedAbsence = new Absence();
            savedAbsence.setStatut(StatutAbsence.EN_ATTENTE);
            savedAbsence.setCollaborateur(employeMock);
            savedAbsence.setTypeAbsence(TypeAbsence.CONGE_PAYE);
            when(absenceRepository.save(any(Absence.class))).thenReturn(savedAbsence);

            CreateAbsenceRequest request = new CreateAbsenceRequest(
                    TypeAbsence.CONGE_PAYE, debut, fin, "Vacances");

            absenceService.creerAbsence(EMPLOYE_ID, request, false);

            ArgumentCaptor<Absence> absenceCaptor = ArgumentCaptor.forClass(Absence.class);
            verify(absenceRepository).save(absenceCaptor.capture());
            assertEquals(StatutAbsence.EN_ATTENTE, absenceCaptor.getValue().getStatut());
        }

        @Test
        @DisplayName("Crée l'absence avec succès quand le type n'est pas soumis à quota")
        void typeNonSoumisAQuota_succesSansVerificationQuota() {
            when(collaborateurRepository.findById(EMPLOYE_ID)).thenReturn(Optional.of(employeMock));

            LocalDate debut = LocalDate.now().plusDays(1);
            LocalDate fin = debut.plusDays(1);

            when(absenceRepository.findChevauchements(eq(EMPLOYE_ID), eq(debut), eq(fin)))
                    .thenReturn(Collections.emptyList());

            Absence savedAbsence = new Absence();
            savedAbsence.setStatut(StatutAbsence.EN_ATTENTE);
            savedAbsence.setCollaborateur(employeMock);
            savedAbsence.setTypeAbsence(TypeAbsence.SANS_SOLDE);
            when(absenceRepository.save(any(Absence.class))).thenReturn(savedAbsence);

            CreateAbsenceRequest request = new CreateAbsenceRequest(TypeAbsence.SANS_SOLDE, debut, fin, null);

            absenceService.creerAbsence(EMPLOYE_ID, request, false);

            verify(absenceRepository).save(any(Absence.class));
            verifyNoInteractions(quotaAbsenceRepository);
        }

        @Test
        @DisplayName("Un manager peut déposer une demande d'absence au même titre qu'un employé")
        void manager_peutDeposerUneDemande_succes() {
            Manager managerDemandeur = mock(Manager.class);
            when(managerDemandeur.getId()).thenReturn(EMPLOYE_ID);

            when(collaborateurRepository.findById(EMPLOYE_ID)).thenReturn(Optional.of(managerDemandeur));

            LocalDate debut = LocalDate.now().plusDays(1);
            LocalDate fin = debut.plusDays(1);

            when(absenceRepository.findChevauchements(eq(EMPLOYE_ID), eq(debut), eq(fin)))
                    .thenReturn(Collections.emptyList());

            Absence savedAbsence = new Absence();
            savedAbsence.setStatut(StatutAbsence.EN_ATTENTE);
            savedAbsence.setCollaborateur(managerDemandeur);
            savedAbsence.setTypeAbsence(TypeAbsence.SANS_SOLDE);
            when(absenceRepository.save(any(Absence.class))).thenReturn(savedAbsence);

            CreateAbsenceRequest request = new CreateAbsenceRequest(TypeAbsence.SANS_SOLDE, debut, fin, null);

            AbsenceDTO dto = absenceService.creerAbsence(EMPLOYE_ID, request, false);

            assertEquals("MANAGER", dto.collaborateurType());
            verify(absenceRepository).save(any(Absence.class));
        }

        @Test
        @DisplayName("Lève AdminNonAutoriseException si le demandeur est ADMIN : il n'a pas d'approbateur")
        void admin_neePeutPasDeposer_lanceException() {
            CreateAbsenceRequest request = new CreateAbsenceRequest(
                    TypeAbsence.SANS_SOLDE, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), null);

            assertThrows(AdminNonAutoriseException.class,
                    () -> absenceService.creerAbsence(EMPLOYE_ID, request, true));

            verifyNoInteractions(absenceRepository, quotaAbsenceRepository, collaborateurRepository);
        }
    }

    @Nested
    @DisplayName("validerAbsence")
    class ValiderAbsence {

        private static final Long ABSENCE_ID = 10L;
        private static final Long MANAGER_ID = 20L;

        @Test
        @DisplayName("Lève ResourceNotFoundException si le manager n'existe pas")
        void managerIntrouvable_lanceException() {
            Absence absence = new Absence();
            absence.setStatut(StatutAbsence.EN_ATTENTE);
            absence.setTypeAbsence(TypeAbsence.CONGE_PAYE);

            when(absenceRepository.findById(ABSENCE_ID)).thenReturn(Optional.of(absence));
            when(managerRepository.findById(MANAGER_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> absenceService.validerAbsence(ABSENCE_ID, MANAGER_ID, false));
        }

        @Test
        @DisplayName("Valide l'absence sans toucher au quota si le type n'y est pas soumis")
        void typeNonSoumisAQuota_valideSansMiseAJourQuota() {
            Absence absence = new Absence();
            absence.setStatut(StatutAbsence.EN_ATTENTE);
            absence.setTypeAbsence(TypeAbsence.SANS_SOLDE);
            absence.setNombreJours(2.0);
            absence.setDateDebut(LocalDate.now());
            absence.setCollaborateur(employeMock);

            Manager manager = mock(Manager.class);
            rattacherEmployeAuManager(MANAGER_ID);

            when(absenceRepository.findById(ABSENCE_ID)).thenReturn(Optional.of(absence));
            when(managerRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
            when(absenceRepository.save(any(Absence.class))).thenAnswer(inv -> inv.getArgument(0));

            absenceService.validerAbsence(ABSENCE_ID, MANAGER_ID, false);

            ArgumentCaptor<Absence> absenceCaptor = ArgumentCaptor.forClass(Absence.class);
            verify(absenceRepository).save(absenceCaptor.capture());
            assertEquals(StatutAbsence.VALIDEE, absenceCaptor.getValue().getStatut());
            verifyNoInteractions(quotaAbsenceRepository);
        }

        @Test
        @DisplayName("Valide l'absence et met à jour joursPris quand le quota est suffisant")
        void typeSoumisAQuota_quotaSuffisant_metAJourJoursPris() {
            LocalDate debut = LocalDate.now();
            Absence absence = new Absence();
            absence.setStatut(StatutAbsence.EN_ATTENTE);
            absence.setTypeAbsence(TypeAbsence.CONGE_PAYE);
            absence.setNombreJours(3.0);
            absence.setDateDebut(debut);
            absence.setCollaborateur(employeMock);

            Manager manager = mock(Manager.class);
            rattacherEmployeAuManager(MANAGER_ID);
            QuotaAbsence quota = mock(QuotaAbsence.class);
            when(quota.getJoursRestants()).thenReturn(5.0);
            when(quota.getJoursPris()).thenReturn(0.0);

            when(absenceRepository.findById(ABSENCE_ID)).thenReturn(Optional.of(absence));
            when(managerRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
            when(quotaAbsenceRepository.findByCollaborateur_IdAndTypeAbsenceAndAnnee(
                    EMPLOYE_ID, TypeAbsence.CONGE_PAYE, debut.getYear())).thenReturn(Optional.of(quota));
            when(absenceRepository.save(any(Absence.class))).thenAnswer(inv -> inv.getArgument(0));

            absenceService.validerAbsence(ABSENCE_ID, MANAGER_ID, false);

            ArgumentCaptor<Absence> absenceCaptor = ArgumentCaptor.forClass(Absence.class);
            verify(absenceRepository).save(absenceCaptor.capture());
            assertEquals(StatutAbsence.VALIDEE, absenceCaptor.getValue().getStatut());

            verify(quota).setJoursPris(3.0);
            verify(quotaAbsenceRepository).save(quota);
        }

        @Test
        @DisplayName("Lève QuotaInsuffisantException si le solde a changé entre la demande et la validation")
        void typeSoumisAQuota_quotaDevenuInsuffisant_lanceException() {
            LocalDate debut = LocalDate.now();
            Absence absence = new Absence();
            absence.setStatut(StatutAbsence.EN_ATTENTE);
            absence.setTypeAbsence(TypeAbsence.CONGE_PAYE);
            absence.setNombreJours(3.0);
            absence.setDateDebut(debut);
            absence.setCollaborateur(employeMock);

            Manager manager = mock(Manager.class);
            rattacherEmployeAuManager(MANAGER_ID);
            QuotaAbsence quota = mock(QuotaAbsence.class);
            when(quota.getJoursRestants()).thenReturn(1.0);

            when(absenceRepository.findById(ABSENCE_ID)).thenReturn(Optional.of(absence));
            when(managerRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
            when(quotaAbsenceRepository.findByCollaborateur_IdAndTypeAbsenceAndAnnee(
                    EMPLOYE_ID, TypeAbsence.CONGE_PAYE, debut.getYear())).thenReturn(Optional.of(quota));

            assertThrows(QuotaInsuffisantException.class,
                    () -> absenceService.validerAbsence(ABSENCE_ID, MANAGER_ID, false));

            verify(quotaAbsenceRepository, never()).save(any());
            verify(absenceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lève IllegalStateException si l'absence n'est pas EN_ATTENTE")
        void absenceDejaTraitee_lanceException() {
            Absence absence = new Absence();
            absence.setStatut(StatutAbsence.VALIDEE);
            absence.setTypeAbsence(TypeAbsence.CONGE_PAYE);
            absence.setCollaborateur(employeMock);

            Manager manager = mock(Manager.class);
            rattacherEmployeAuManager(MANAGER_ID);

            when(absenceRepository.findById(ABSENCE_ID)).thenReturn(Optional.of(absence));
            when(managerRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));

            assertThrows(IllegalStateException.class,
                    () -> absenceService.validerAbsence(ABSENCE_ID, MANAGER_ID, false));
        }

        @Test
        @DisplayName("Lève ManagerNonAutoriseException si un manager pair tente de valider la demande d'un autre manager")
        void demandeDeManager_managerPairNonAdmin_lanceException() {
            Manager demandeur = mock(Manager.class);
            Absence absence = new Absence();
            absence.setStatut(StatutAbsence.EN_ATTENTE);
            absence.setTypeAbsence(TypeAbsence.SANS_SOLDE);
            absence.setCollaborateur(demandeur);

            Manager validateur = mock(Manager.class);

            when(absenceRepository.findById(ABSENCE_ID)).thenReturn(Optional.of(absence));
            when(managerRepository.findById(MANAGER_ID)).thenReturn(Optional.of(validateur));

            assertThrows(ManagerNonAutoriseException.class,
                    () -> absenceService.validerAbsence(ABSENCE_ID, MANAGER_ID, false));

            verify(absenceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Un ADMIN peut valider la demande d'absence d'un manager")
        void demandeDeManager_admin_succes() {
            Manager demandeur = mock(Manager.class);
            Absence absence = new Absence();
            absence.setStatut(StatutAbsence.EN_ATTENTE);
            absence.setTypeAbsence(TypeAbsence.SANS_SOLDE);
            absence.setCollaborateur(demandeur);

            Manager validateurAdmin = mock(Manager.class);

            when(absenceRepository.findById(ABSENCE_ID)).thenReturn(Optional.of(absence));
            when(managerRepository.findById(MANAGER_ID)).thenReturn(Optional.of(validateurAdmin));
            when(absenceRepository.save(any(Absence.class))).thenAnswer(inv -> inv.getArgument(0));

            AbsenceDTO dto = absenceService.validerAbsence(ABSENCE_ID, MANAGER_ID, true);

            assertEquals(StatutAbsence.VALIDEE, dto.statut());
        }

        @Test
        @DisplayName("Lève ManagerNonAutoriseException si l'employé n'appartient pas à l'équipe du valideur")
        void employeHorsEquipe_lanceException() {
            Absence absence = new Absence();
            absence.setStatut(StatutAbsence.EN_ATTENTE);
            absence.setTypeAbsence(TypeAbsence.SANS_SOLDE);
            absence.setCollaborateur(employeMock);

            rattacherEmployeAuManager(999L); // l'employé relève d'un autre manager

            when(absenceRepository.findById(ABSENCE_ID)).thenReturn(Optional.of(absence));
            when(managerRepository.findById(MANAGER_ID)).thenReturn(Optional.of(mock(Manager.class)));

            assertThrows(ManagerNonAutoriseException.class,
                    () -> absenceService.validerAbsence(ABSENCE_ID, MANAGER_ID, false));

            verify(absenceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lève ManagerNonAutoriseException si l'employé n'a aucun manager rattaché")
        void employeSansManager_lanceException() {
            Absence absence = new Absence();
            absence.setStatut(StatutAbsence.EN_ATTENTE);
            absence.setTypeAbsence(TypeAbsence.SANS_SOLDE);
            absence.setCollaborateur(employeMock);

            when(employeMock.getManager()).thenReturn(null);
            when(absenceRepository.findById(ABSENCE_ID)).thenReturn(Optional.of(absence));
            when(managerRepository.findById(MANAGER_ID)).thenReturn(Optional.of(mock(Manager.class)));

            assertThrows(ManagerNonAutoriseException.class,
                    () -> absenceService.validerAbsence(ABSENCE_ID, MANAGER_ID, false));

            verify(absenceRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("rejeterAbsence")
    class RejeterAbsence {

        private static final Long ABSENCE_ID = 11L;
        private static final Long MANAGER_ID = 21L;

        @Test
        @DisplayName("Un manager peut rejeter la demande d'un employé")
        void demandeDEmploye_manager_succes() {
            Absence absence = new Absence();
            absence.setStatut(StatutAbsence.EN_ATTENTE);
            absence.setTypeAbsence(TypeAbsence.SANS_SOLDE);
            absence.setCollaborateur(employeMock);

            Manager manager = mock(Manager.class);
            rattacherEmployeAuManager(MANAGER_ID);

            when(absenceRepository.findById(ABSENCE_ID)).thenReturn(Optional.of(absence));
            when(managerRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
            when(absenceRepository.save(any(Absence.class))).thenAnswer(inv -> inv.getArgument(0));

            AbsenceDTO dto = absenceService.rejeterAbsence(ABSENCE_ID, MANAGER_ID, "Motif", false);

            assertEquals(StatutAbsence.REJETEE, dto.statut());
            assertEquals("Motif", dto.motifRejet());
        }

        @Test
        @DisplayName("Lève ManagerNonAutoriseException si un manager pair tente de rejeter la demande d'un autre manager")
        void demandeDeManager_managerPairNonAdmin_lanceException() {
            Manager demandeur = mock(Manager.class);
            Absence absence = new Absence();
            absence.setStatut(StatutAbsence.EN_ATTENTE);
            absence.setTypeAbsence(TypeAbsence.SANS_SOLDE);
            absence.setCollaborateur(demandeur);

            Manager validateur = mock(Manager.class);

            when(absenceRepository.findById(ABSENCE_ID)).thenReturn(Optional.of(absence));
            when(managerRepository.findById(MANAGER_ID)).thenReturn(Optional.of(validateur));

            assertThrows(ManagerNonAutoriseException.class,
                    () -> absenceService.rejeterAbsence(ABSENCE_ID, MANAGER_ID, "Motif", false));

            verify(absenceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Un ADMIN peut rejeter la demande d'absence d'un manager")
        void demandeDeManager_admin_succes() {
            Manager demandeur = mock(Manager.class);
            Absence absence = new Absence();
            absence.setStatut(StatutAbsence.EN_ATTENTE);
            absence.setTypeAbsence(TypeAbsence.SANS_SOLDE);
            absence.setCollaborateur(demandeur);

            Manager validateurAdmin = mock(Manager.class);

            when(absenceRepository.findById(ABSENCE_ID)).thenReturn(Optional.of(absence));
            when(managerRepository.findById(MANAGER_ID)).thenReturn(Optional.of(validateurAdmin));
            when(absenceRepository.save(any(Absence.class))).thenAnswer(inv -> inv.getArgument(0));

            AbsenceDTO dto = absenceService.rejeterAbsence(ABSENCE_ID, MANAGER_ID, "Motif admin", true);

            assertEquals(StatutAbsence.REJETEE, dto.statut());
        }

        @Test
        @DisplayName("Lève ManagerNonAutoriseException si l'employé n'appartient pas à l'équipe du valideur")
        void employeHorsEquipe_lanceException() {
            Absence absence = new Absence();
            absence.setStatut(StatutAbsence.EN_ATTENTE);
            absence.setTypeAbsence(TypeAbsence.SANS_SOLDE);
            absence.setCollaborateur(employeMock);

            rattacherEmployeAuManager(999L); // l'employé relève d'un autre manager

            when(absenceRepository.findById(ABSENCE_ID)).thenReturn(Optional.of(absence));
            when(managerRepository.findById(MANAGER_ID)).thenReturn(Optional.of(mock(Manager.class)));

            assertThrows(ManagerNonAutoriseException.class,
                    () -> absenceService.rejeterAbsence(ABSENCE_ID, MANAGER_ID, "Motif", false));

            verify(absenceRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("listerEnAttente")
    class ListerEnAttente {

        private static final Long MANAGER_ID = 30L;
        private static final Long AUTRE_MANAGER_ID = 31L;

        @Test
        @DisplayName("Un manager ne voit que les demandes de ses propres employés (jamais les siennes, jamais celles d'un autre manager)")
        void manager_neVoitQueSesEmployes() {
            Manager sonManager = mock(Manager.class);
            when(sonManager.getId()).thenReturn(MANAGER_ID);
            Manager autreManager = mock(Manager.class);
            when(autreManager.getId()).thenReturn(AUTRE_MANAGER_ID);

            Employe sonEmploye = new Employe();
            sonEmploye.setId(1L);
            sonEmploye.setManager(sonManager);

            Employe autreEmploye = new Employe();
            autreEmploye.setId(2L);
            autreEmploye.setManager(autreManager);

            Manager demandeurManager = mock(Manager.class);

            Absence absenceDeSonEmploye = new Absence();
            absenceDeSonEmploye.setStatut(StatutAbsence.EN_ATTENTE);
            absenceDeSonEmploye.setTypeAbsence(TypeAbsence.SANS_SOLDE);
            absenceDeSonEmploye.setCollaborateur(sonEmploye);

            Absence absenceDAutreEmploye = new Absence();
            absenceDAutreEmploye.setStatut(StatutAbsence.EN_ATTENTE);
            absenceDAutreEmploye.setCollaborateur(autreEmploye);

            Absence absenceDeManager = new Absence();
            absenceDeManager.setStatut(StatutAbsence.EN_ATTENTE);
            absenceDeManager.setCollaborateur(demandeurManager);

            when(absenceRepository.findByStatut(StatutAbsence.EN_ATTENTE))
                    .thenReturn(List.of(absenceDeSonEmploye, absenceDAutreEmploye, absenceDeManager));

            List<AbsenceDTO> resultat = absenceService.listerEnAttente(MANAGER_ID, false);

            assertEquals(1, resultat.size());
            assertEquals(1L, resultat.get(0).employeId());
        }

        @Test
        @DisplayName("Un ADMIN ne voit que les demandes des managers, jamais celles des employés")
        void admin_neVoitQueLesManagers() {
            Manager unManager = mock(Manager.class);

            Manager sonManagerAssocie = mock(Manager.class);
            Employe unEmploye = new Employe();
            unEmploye.setId(1L);
            unEmploye.setManager(sonManagerAssocie);

            Absence absenceDeManager = new Absence();
            absenceDeManager.setStatut(StatutAbsence.EN_ATTENTE);
            absenceDeManager.setTypeAbsence(TypeAbsence.SANS_SOLDE);
            absenceDeManager.setCollaborateur(unManager);

            Absence absenceDEmploye = new Absence();
            absenceDEmploye.setStatut(StatutAbsence.EN_ATTENTE);
            absenceDEmploye.setCollaborateur(unEmploye);

            when(absenceRepository.findByStatut(StatutAbsence.EN_ATTENTE))
                    .thenReturn(List.of(absenceDeManager, absenceDEmploye));

            List<AbsenceDTO> resultat = absenceService.listerEnAttente(999L, true);

            assertEquals(1, resultat.size());
            assertEquals("MANAGER", resultat.get(0).collaborateurType());
        }
    }
}
