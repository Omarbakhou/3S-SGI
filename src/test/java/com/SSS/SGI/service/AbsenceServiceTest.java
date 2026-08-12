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
import com.SSS.SGI.exception.QuotaInsuffisantException;
import com.SSS.SGI.exception.ResourceNotFoundException;
import com.SSS.SGI.repository.AbsenceRepository;
import com.SSS.SGI.repository.EmployeRepository;
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
    private EmployeRepository employeRepository;
    @Mock
    private ManagerRepository managerRepository;

    private AbsenceService absenceService;

    private Employe employeMock;
    private static final Long EMPLOYE_ID = 1L;

    @BeforeEach
    void setUp() {
        absenceService = new AbsenceService(
                absenceRepository, quotaAbsenceRepository, employeRepository, managerRepository);
        ReflectionTestUtils.setField(absenceService, "dossierJustificatifs", "./justificatifs-test");

        employeMock = mock(Employe.class);
        lenient().when(employeMock.getId()).thenReturn(EMPLOYE_ID);
        lenient().when(employeMock.getNom()).thenReturn("Doe");
        lenient().when(employeMock.getPrenom()).thenReturn("Jane");
    }

    @Nested
    @DisplayName("creerAbsence")
    class CreerAbsence {

        @Test
        @DisplayName("Lève ResourceNotFoundException si l'employé n'existe pas")
        void employeIntrouvable_lanceException() {
            when(employeRepository.findById(EMPLOYE_ID)).thenReturn(Optional.empty());

            CreateAbsenceRequest request = new CreateAbsenceRequest(
                    TypeAbsence.CONGE_PAYE, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), null);

            assertThrows(ResourceNotFoundException.class,
                    () -> absenceService.creerAbsence(EMPLOYE_ID, request));

            verifyNoInteractions(absenceRepository, quotaAbsenceRepository);
        }

        @Test
        @DisplayName("Lève IllegalArgumentException si la date de fin précède la date de début")
        void dateFinAvantDateDebut_lanceException() {
            when(employeRepository.findById(EMPLOYE_ID)).thenReturn(Optional.of(employeMock));

            CreateAbsenceRequest request = new CreateAbsenceRequest(
                    TypeAbsence.CONGE_PAYE, LocalDate.now().plusDays(5), LocalDate.now().plusDays(1), null);

            assertThrows(IllegalArgumentException.class,
                    () -> absenceService.creerAbsence(EMPLOYE_ID, request));
        }

        @Test
        @DisplayName("Lève IllegalArgumentException si la date de début est dans le passé")
        void dateDansLePasse_lanceException() {
            when(employeRepository.findById(EMPLOYE_ID)).thenReturn(Optional.of(employeMock));

            CreateAbsenceRequest request = new CreateAbsenceRequest(
                    TypeAbsence.CONGE_PAYE, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), null);

            assertThrows(IllegalArgumentException.class,
                    () -> absenceService.creerAbsence(EMPLOYE_ID, request));
        }

        @Test
        @DisplayName("Lève IllegalArgumentException si la durée dépasse 90 jours")
        void dureeSuperieureA90Jours_lanceException() {
            when(employeRepository.findById(EMPLOYE_ID)).thenReturn(Optional.of(employeMock));

            CreateAbsenceRequest request = new CreateAbsenceRequest(
                    TypeAbsence.CONGE_PAYE, LocalDate.now().plusDays(1), LocalDate.now().plusDays(100), null);

            assertThrows(IllegalArgumentException.class,
                    () -> absenceService.creerAbsence(EMPLOYE_ID, request));
        }

        @Test
        @DisplayName("Lève AbsenceChevauchementException si une absence existe déjà sur la période")
        void chevauchementExistant_lanceException() {
            when(employeRepository.findById(EMPLOYE_ID)).thenReturn(Optional.of(employeMock));

            LocalDate debut = LocalDate.now().plusDays(1);
            LocalDate fin = LocalDate.now().plusDays(3);

            when(absenceRepository.findChevauchements(EMPLOYE_ID, debut, fin))
                    .thenReturn(List.of(mock(Absence.class)));

            CreateAbsenceRequest request = new CreateAbsenceRequest(TypeAbsence.CONGE_PAYE, debut, fin, null);

            assertThrows(AbsenceChevauchementException.class,
                    () -> absenceService.creerAbsence(EMPLOYE_ID, request));

            verifyNoInteractions(quotaAbsenceRepository);
        }

        @Test
        @DisplayName("Lève QuotaInsuffisantException si aucun quota n'est défini pour un type soumis à quota")
        void typeSoumisAQuota_quotaInexistant_lanceException() {
            when(employeRepository.findById(EMPLOYE_ID)).thenReturn(Optional.of(employeMock));

            LocalDate debut = LocalDate.now().plusDays(1);
            LocalDate fin = LocalDate.now().plusDays(2);

            when(absenceRepository.findChevauchements(EMPLOYE_ID, debut, fin))
                    .thenReturn(Collections.emptyList());

            when(quotaAbsenceRepository.findByEmploye_IdAndTypeAbsenceAndAnnee(
                    EMPLOYE_ID, TypeAbsence.CONGE_PAYE, debut.getYear()))
                    .thenReturn(Optional.empty());

            CreateAbsenceRequest request = new CreateAbsenceRequest(TypeAbsence.CONGE_PAYE, debut, fin, null);

            assertThrows(QuotaInsuffisantException.class,
                    () -> absenceService.creerAbsence(EMPLOYE_ID, request));

            verify(absenceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lève QuotaInsuffisantException si le solde de quota est insuffisant")
        void typeSoumisAQuota_quotaInsuffisant_lanceException() {
            when(employeRepository.findById(EMPLOYE_ID)).thenReturn(Optional.of(employeMock));

            LocalDate debut = LocalDate.now().plusDays(1);
            LocalDate fin = debut.plusDays(4);

            when(absenceRepository.findChevauchements(eq(EMPLOYE_ID), eq(debut), eq(fin)))
                    .thenReturn(Collections.emptyList());

            QuotaAbsence quota = mock(QuotaAbsence.class);
            when(quota.getJoursRestants()).thenReturn(1.0);

            when(quotaAbsenceRepository.findByEmploye_IdAndTypeAbsenceAndAnnee(
                    EMPLOYE_ID, TypeAbsence.CONGE_PAYE, debut.getYear()))
                    .thenReturn(Optional.of(quota));

            CreateAbsenceRequest request = new CreateAbsenceRequest(TypeAbsence.CONGE_PAYE, debut, fin, null);

            assertThrows(QuotaInsuffisantException.class,
                    () -> absenceService.creerAbsence(EMPLOYE_ID, request));

            verify(absenceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Crée l'absence avec succès quand le quota est suffisant")
        void typeSoumisAQuota_quotaSuffisant_succes() {
            when(employeRepository.findById(EMPLOYE_ID)).thenReturn(Optional.of(employeMock));

            LocalDate debut = LocalDate.now().plusDays(1);
            LocalDate fin = debut.plusDays(1);

            when(absenceRepository.findChevauchements(eq(EMPLOYE_ID), eq(debut), eq(fin)))
                    .thenReturn(Collections.emptyList());

            QuotaAbsence quota = mock(QuotaAbsence.class);
            when(quota.getJoursRestants()).thenReturn(10.0);

            when(quotaAbsenceRepository.findByEmploye_IdAndTypeAbsenceAndAnnee(
                    EMPLOYE_ID, TypeAbsence.CONGE_PAYE, debut.getYear()))
                    .thenReturn(Optional.of(quota));

            Absence savedAbsence = new Absence();
            savedAbsence.setStatut(StatutAbsence.EN_ATTENTE);
            savedAbsence.setEmploye(employeMock);
            savedAbsence.setTypeAbsence(TypeAbsence.CONGE_PAYE);
            when(absenceRepository.save(any(Absence.class))).thenReturn(savedAbsence);

            CreateAbsenceRequest request = new CreateAbsenceRequest(
                    TypeAbsence.CONGE_PAYE, debut, fin, "Vacances");

            absenceService.creerAbsence(EMPLOYE_ID, request);

            ArgumentCaptor<Absence> absenceCaptor = ArgumentCaptor.forClass(Absence.class);
            verify(absenceRepository).save(absenceCaptor.capture());
            assertEquals(StatutAbsence.EN_ATTENTE, absenceCaptor.getValue().getStatut());
        }

        @Test
        @DisplayName("Crée l'absence avec succès quand le type n'est pas soumis à quota")
        void typeNonSoumisAQuota_succesSansVerificationQuota() {
            when(employeRepository.findById(EMPLOYE_ID)).thenReturn(Optional.of(employeMock));

            LocalDate debut = LocalDate.now().plusDays(1);
            LocalDate fin = debut.plusDays(1);

            when(absenceRepository.findChevauchements(eq(EMPLOYE_ID), eq(debut), eq(fin)))
                    .thenReturn(Collections.emptyList());

            Absence savedAbsence = new Absence();
            savedAbsence.setStatut(StatutAbsence.EN_ATTENTE);
            savedAbsence.setEmploye(employeMock);
            savedAbsence.setTypeAbsence(TypeAbsence.SANS_SOLDE);
            when(absenceRepository.save(any(Absence.class))).thenReturn(savedAbsence);

            CreateAbsenceRequest request = new CreateAbsenceRequest(TypeAbsence.SANS_SOLDE, debut, fin, null);

            absenceService.creerAbsence(EMPLOYE_ID, request);

            verify(absenceRepository).save(any(Absence.class));
            verifyNoInteractions(quotaAbsenceRepository);
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
                    () -> absenceService.validerAbsence(ABSENCE_ID, MANAGER_ID));
        }

        @Test
        @DisplayName("Valide l'absence sans toucher au quota si le type n'y est pas soumis")
        void typeNonSoumisAQuota_valideSansMiseAJourQuota() {
            Absence absence = new Absence();
            absence.setStatut(StatutAbsence.EN_ATTENTE);
            absence.setTypeAbsence(TypeAbsence.SANS_SOLDE);
            absence.setNombreJours(2.0);
            absence.setDateDebut(LocalDate.now());
            absence.setEmploye(employeMock);

            Manager manager = mock(Manager.class);

            when(absenceRepository.findById(ABSENCE_ID)).thenReturn(Optional.of(absence));
            when(managerRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
            when(absenceRepository.save(any(Absence.class))).thenAnswer(inv -> inv.getArgument(0));

            absenceService.validerAbsence(ABSENCE_ID, MANAGER_ID);

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
            absence.setEmploye(employeMock);

            Manager manager = mock(Manager.class);
            QuotaAbsence quota = mock(QuotaAbsence.class);
            when(quota.getJoursRestants()).thenReturn(5.0);
            when(quota.getJoursPris()).thenReturn(0.0);

            when(absenceRepository.findById(ABSENCE_ID)).thenReturn(Optional.of(absence));
            when(managerRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
            when(quotaAbsenceRepository.findByEmploye_IdAndTypeAbsenceAndAnnee(
                    EMPLOYE_ID, TypeAbsence.CONGE_PAYE, debut.getYear())).thenReturn(Optional.of(quota));
            when(absenceRepository.save(any(Absence.class))).thenAnswer(inv -> inv.getArgument(0));

            absenceService.validerAbsence(ABSENCE_ID, MANAGER_ID);

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
            absence.setEmploye(employeMock);

            Manager manager = mock(Manager.class);
            QuotaAbsence quota = mock(QuotaAbsence.class);
            when(quota.getJoursRestants()).thenReturn(1.0);

            when(absenceRepository.findById(ABSENCE_ID)).thenReturn(Optional.of(absence));
            when(managerRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
            when(quotaAbsenceRepository.findByEmploye_IdAndTypeAbsenceAndAnnee(
                    EMPLOYE_ID, TypeAbsence.CONGE_PAYE, debut.getYear())).thenReturn(Optional.of(quota));

            assertThrows(QuotaInsuffisantException.class,
                    () -> absenceService.validerAbsence(ABSENCE_ID, MANAGER_ID));

            verify(quotaAbsenceRepository, never()).save(any());
            verify(absenceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lève IllegalStateException si l'absence n'est pas EN_ATTENTE")
        void absenceDejaTraitee_lanceException() {
            Absence absence = new Absence();
            absence.setStatut(StatutAbsence.VALIDEE);
            absence.setTypeAbsence(TypeAbsence.CONGE_PAYE);
            absence.setEmploye(employeMock);

            Manager manager = mock(Manager.class);

            when(absenceRepository.findById(ABSENCE_ID)).thenReturn(Optional.of(absence));
            when(managerRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));

            assertThrows(IllegalStateException.class,
                    () -> absenceService.validerAbsence(ABSENCE_ID, MANAGER_ID));
        }
    }
}