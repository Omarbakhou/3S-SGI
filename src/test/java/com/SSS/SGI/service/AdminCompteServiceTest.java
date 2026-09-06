package com.SSS.SGI.service;

import com.SSS.SGI.entity.Collaborateur;
import com.SSS.SGI.entity.Employe;
import com.SSS.SGI.entity.Manager;
import com.SSS.SGI.exception.ResourceNotFoundException;
import com.SSS.SGI.repository.CollaborateurRepository;
import com.SSS.SGI.security.AdminEmails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Règles métier de la (dés)activation de comptes : garde-fous et absence de
 * suppression physique.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminCompteService")
class AdminCompteServiceTest {

    private static final String ADMIN_EMAIL = "admin@3s-sgi.local";
    private static final String AUTRE_ADMIN_EMAIL = "admin2@3s-sgi.local";

    @Mock private CollaborateurRepository collaborateurRepository;

    private AdminCompteService service;

    private Manager admin;
    private Manager autreAdmin;
    private Employe employe;

    @BeforeEach
    void setUp() {
        service = new AdminCompteService(collaborateurRepository, new AdminEmails(ADMIN_EMAIL + "," + AUTRE_ADMIN_EMAIL));

        admin = new Manager();
        admin.setId(1L);
        admin.setNom("Root");
        admin.setPrenom("Ada");
        admin.setEmail(ADMIN_EMAIL);
        admin.setActif(true);

        autreAdmin = new Manager();
        autreAdmin.setId(2L);
        autreAdmin.setNom("Second");
        autreAdmin.setPrenom("Bob");
        autreAdmin.setEmail(AUTRE_ADMIN_EMAIL);
        autreAdmin.setActif(true);

        employe = new Employe();
        employe.setId(3L);
        employe.setNom("Doe");
        employe.setPrenom("Jane");
        employe.setEmail("jane.doe@3s-sgi.local");
        employe.setActif(true);
    }

    @Nested
    @DisplayName("Désactivation")
    class Desactivation {

        @Test
        @DisplayName("bascule le compte à inactif sans le supprimer")
        void desactive_sansSupprimer() {
            when(collaborateurRepository.findById(3L)).thenReturn(Optional.of(employe));
            when(collaborateurRepository.save(any(Collaborateur.class))).thenAnswer(i -> i.getArgument(0));

            Collaborateur resultat = service.desactiver(3L, 1L);

            assertFalse(resultat.isActif(), "le compte doit être marqué inactif");
            verify(collaborateurRepository).save(employe);
            verify(collaborateurRepository, never()).delete(any());
            verify(collaborateurRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("refuse qu'un admin désactive son propre compte")
        void refuse_autoDesactivation() {
            IllegalStateException erreur = assertThrows(
                    IllegalStateException.class,
                    () -> service.desactiver(1L, 1L));

            assertTrue(erreur.getMessage().contains("votre propre compte"));
            // Le garde-fou doit agir avant tout accès en base.
            verifyNoInteractions(collaborateurRepository);
        }

        @Test
        @DisplayName("refuse de désactiver le dernier administrateur actif")
        void refuse_dernierAdmin() {
            autreAdmin.setActif(false); // il ne reste qu'un seul admin actif : `admin`
            when(collaborateurRepository.findById(1L)).thenReturn(Optional.of(admin));
            when(collaborateurRepository.findAll()).thenReturn(List.of(admin, autreAdmin, employe));

            IllegalStateException erreur = assertThrows(
                    IllegalStateException.class,
                    () -> service.desactiver(1L, 2L));

            assertTrue(erreur.getMessage().contains("dernier compte administrateur"));
            verify(collaborateurRepository, never()).save(any());
        }

        @Test
        @DisplayName("autorise la désactivation d'un admin tant qu'il en reste un autre actif")
        void autorise_siAutreAdminActif() {
            when(collaborateurRepository.findById(1L)).thenReturn(Optional.of(admin));
            when(collaborateurRepository.findAll()).thenReturn(List.of(admin, autreAdmin, employe));
            when(collaborateurRepository.save(any(Collaborateur.class))).thenAnswer(i -> i.getArgument(0));

            Collaborateur resultat = service.desactiver(1L, 2L);

            assertFalse(resultat.isActif());
        }

        @Test
        @DisplayName("un employé n'est jamais concerné par la règle du dernier administrateur")
        void employe_pasConcerneParRegleAdmin() {
            when(collaborateurRepository.findById(3L)).thenReturn(Optional.of(employe));
            when(collaborateurRepository.save(any(Collaborateur.class))).thenAnswer(i -> i.getArgument(0));

            assertFalse(service.desactiver(3L, 1L).isActif());
            // Inutile de recenser les admins pour un employé.
            verify(collaborateurRepository, never()).findAll();
        }

        @Test
        @DisplayName("est sans effet sur un compte déjà inactif")
        void idempotent() {
            employe.setActif(false);
            when(collaborateurRepository.findById(3L)).thenReturn(Optional.of(employe));

            assertFalse(service.desactiver(3L, 1L).isActif());
            verify(collaborateurRepository, never()).save(any());
        }

        @Test
        @DisplayName("échoue si le compte cible n'existe pas")
        void compteInconnu() {
            when(collaborateurRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.desactiver(99L, 1L));
        }
    }

    @Nested
    @DisplayName("Réactivation")
    class Reactivation {

        @Test
        @DisplayName("rend l'accès à un compte désactivé")
        void reactive() {
            employe.setActif(false);
            when(collaborateurRepository.findById(3L)).thenReturn(Optional.of(employe));
            when(collaborateurRepository.save(any(Collaborateur.class))).thenAnswer(i -> i.getArgument(0));

            assertTrue(service.reactiver(3L).isActif());
        }

        @Test
        @DisplayName("est sans effet sur un compte déjà actif")
        void idempotent() {
            when(collaborateurRepository.findById(3L)).thenReturn(Optional.of(employe));

            assertTrue(service.reactiver(3L).isActif());
            verify(collaborateurRepository, never()).save(any());
        }
    }
}
