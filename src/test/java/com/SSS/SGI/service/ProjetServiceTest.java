package com.SSS.SGI.service;

import com.SSS.SGI.entity.BudgetProjet;
import com.SSS.SGI.entity.Client;
import com.SSS.SGI.entity.Projet;
import com.SSS.SGI.repository.BudgetProjetRepository;
import com.SSS.SGI.repository.ClientRepository;
import com.SSS.SGI.repository.ProjetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjetServiceTest {

    @Mock
    private ProjetRepository projetRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private BudgetProjetRepository budgetProjetRepository;

    private ProjetService projetService;

    private static final Long CLIENT_ID = 1L;
    private static final Long PROJET_ID = 10L;

    private Client client;

    @BeforeEach
    void setUp() {
        projetService = new ProjetService(projetRepository, clientRepository, budgetProjetRepository);

        client = new Client();
        client.setId(CLIENT_ID);
        client.setNomClient("Acme Corp");
    }

    @Nested
    @DisplayName("createClient")
    class CreateClient {

        @Test
        @DisplayName("Crée le client avec succès quand le nom est renseigné")
        void nomRenseigne_succes() {
            Client nouveau = new Client();
            nouveau.setNomClient("Acme Corp");

            when(clientRepository.save(nouveau)).thenReturn(client);

            Client resultat = projetService.createClient(nouveau);

            assertEquals(CLIENT_ID, resultat.getId());
            verify(clientRepository).save(nouveau);
        }

        @Test
        @DisplayName("Lève IllegalArgumentException si le nom est null")
        void nomNull_lanceException() {
            Client nouveau = new Client();
            nouveau.setNomClient(null);

            assertThrows(IllegalArgumentException.class, () -> projetService.createClient(nouveau));
            verifyNoInteractions(clientRepository);
        }

        @Test
        @DisplayName("Lève IllegalArgumentException si le nom est vide ou blanc")
        void nomBlanc_lanceException() {
            Client nouveau = new Client();
            nouveau.setNomClient("   ");

            assertThrows(IllegalArgumentException.class, () -> projetService.createClient(nouveau));
            verifyNoInteractions(clientRepository);
        }
    }

    @Nested
    @DisplayName("getClientById / getAllClients / findByNomClient")
    class ReadClient {

        @Test
        @DisplayName("Retourne le client s'il existe")
        void clientExiste_retourneClient() {
            when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(client));

            Optional<Client> resultat = projetService.getClientById(CLIENT_ID);

            assertTrue(resultat.isPresent());
            assertEquals("Acme Corp", resultat.get().getNomClient());
        }

        @Test
        @DisplayName("Retourne Optional vide si le client n'existe pas")
        void clientInexistant_retourneVide() {
            when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.empty());

            assertTrue(projetService.getClientById(CLIENT_ID).isEmpty());
        }

        @Test
        @DisplayName("Retourne tous les clients")
        void retourneTousLesClients() {
            when(clientRepository.findAll()).thenReturn(List.of(client));

            List<Client> resultat = projetService.getAllClients();

            assertEquals(1, resultat.size());
            verify(clientRepository).findAll();
        }

        @Test
        @DisplayName("Retourne le client par son nom")
        void retourneClientParNom() {
            when(clientRepository.findByNomClient("Acme Corp")).thenReturn(Optional.of(client));

            Optional<Client> resultat = projetService.findByNomClient("Acme Corp");

            assertTrue(resultat.isPresent());
            assertEquals(CLIENT_ID, resultat.get().getId());
        }
    }

    @Nested
    @DisplayName("updateClient")
    class UpdateClient {

        @Test
        @DisplayName("Met à jour le nom du client existant")
        void clientExiste_metAJour() {
            when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(client));
            when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

            Client miseAJour = new Client();
            miseAJour.setNomClient("Nouveau Nom");

            Client resultat = projetService.updateClient(CLIENT_ID, miseAJour);

            assertEquals("Nouveau Nom", resultat.getNomClient());
            verify(clientRepository).save(client);
        }

        @Test
        @DisplayName("Lève IllegalArgumentException si le client n'existe pas")
        void clientInexistant_lanceException() {
            when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.empty());

            Client miseAJour = new Client();
            miseAJour.setNomClient("Nouveau Nom");

            assertThrows(IllegalArgumentException.class,
                    () -> projetService.updateClient(CLIENT_ID, miseAJour));
            verify(clientRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteClient")
    class DeleteClient {

        @Test
        @DisplayName("Supprime le client par son ID")
        void supprimeClient() {
            projetService.deleteClient(CLIENT_ID);

            verify(clientRepository).deleteById(CLIENT_ID);
        }
    }

    @Nested
    @DisplayName("createProjet")
    class CreateProjet {

        @Test
        @DisplayName("Crée le projet avec succès quand un client est fourni")
        void clientFourni_succes() {
            Projet projet = new Projet();
            projet.setNom("Refonte SI");
            projet.setClient(client);

            when(projetRepository.save(projet)).thenReturn(projet);

            Projet resultat = projetService.createProjet(projet);

            assertEquals("Refonte SI", resultat.getNom());
            verify(projetRepository).save(projet);
        }

        @Test
        @DisplayName("Lève IllegalArgumentException si aucun client n'est fourni")
        void sansClient_lanceException() {
            Projet projet = new Projet();
            projet.setNom("Refonte SI");

            assertThrows(IllegalArgumentException.class, () -> projetService.createProjet(projet));
            verifyNoInteractions(projetRepository);
        }
    }

    @Nested
    @DisplayName("getProjetById / findProjetByNom / getAllProjets / getProjetsByClient")
    class ReadProjet {

        @Test
        @DisplayName("Retourne le projet s'il existe")
        void projetExiste_retourneProjet() {
            Projet projet = new Projet();
            projet.setId(PROJET_ID);
            projet.setNom("Refonte SI");

            when(projetRepository.findById(PROJET_ID)).thenReturn(Optional.of(projet));

            Optional<Projet> resultat = projetService.getProjetById(PROJET_ID);

            assertTrue(resultat.isPresent());
            assertEquals("Refonte SI", resultat.get().getNom());
        }

        @Test
        @DisplayName("Retourne Optional vide si le projet n'existe pas")
        void projetInexistant_retourneVide() {
            when(projetRepository.findById(PROJET_ID)).thenReturn(Optional.empty());

            assertTrue(projetService.getProjetById(PROJET_ID).isEmpty());
        }

        @Test
        @DisplayName("Retourne le projet par son nom")
        void retourneProjetParNom() {
            Projet projet = new Projet();
            projet.setNom("Refonte SI");

            when(projetRepository.findProjetByNom("Refonte SI")).thenReturn(Optional.of(projet));

            Optional<Projet> resultat = projetService.findProjetByNom("Refonte SI");

            assertTrue(resultat.isPresent());
        }

        @Test
        @DisplayName("Retourne tous les projets")
        void retourneTousLesProjets() {
            when(projetRepository.findAll()).thenReturn(List.of(new Projet(), new Projet()));

            List<Projet> resultat = projetService.getAllProjets();

            assertEquals(2, resultat.size());
        }

        @Test
        @DisplayName("Retourne les projets d'un client")
        void retourneProjetsDuClient() {
            when(projetRepository.findProjetByClientId(CLIENT_ID)).thenReturn(List.of(new Projet()));

            List<Projet> resultat = projetService.getProjetsByClient(CLIENT_ID);

            assertEquals(1, resultat.size());
            verify(projetRepository).findProjetByClientId(CLIENT_ID);
        }
    }

    @Nested
    @DisplayName("updateProjet")
    class UpdateProjet {

        @Test
        @DisplayName("Met à jour le nom et les dates du projet existant")
        void projetExiste_metAJour() {
            Projet existant = new Projet();
            existant.setId(PROJET_ID);
            existant.setNom("Ancien Nom");

            when(projetRepository.findById(PROJET_ID)).thenReturn(Optional.of(existant));
            when(projetRepository.save(any(Projet.class))).thenAnswer(inv -> inv.getArgument(0));

            Projet miseAJour = new Projet();
            miseAJour.setNom("Nouveau Nom");
            miseAJour.setDateDebut(LocalDate.of(2026, 1, 1));
            miseAJour.setDateFin(LocalDate.of(2026, 12, 31));

            Projet resultat = projetService.updateProjet(PROJET_ID, miseAJour);

            assertEquals("Nouveau Nom", resultat.getNom());
            assertEquals(LocalDate.of(2026, 1, 1), resultat.getDateDebut());
            assertEquals(LocalDate.of(2026, 12, 31), resultat.getDateFin());
            verify(projetRepository).save(existant);
        }

        @Test
        @DisplayName("Lève IllegalArgumentException si le projet n'existe pas")
        void projetInexistant_lanceException() {
            when(projetRepository.findById(PROJET_ID)).thenReturn(Optional.empty());

            Projet miseAJour = new Projet();
            miseAJour.setNom("Nouveau Nom");

            assertThrows(IllegalArgumentException.class,
                    () -> projetService.updateProjet(PROJET_ID, miseAJour));
            verify(projetRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteProjet")
    class DeleteProjet {

        @Test
        @DisplayName("Supprime le projet par son ID")
        void supprimeProjet() {
            projetService.deleteProjet(PROJET_ID);

            verify(projetRepository).deleteById(PROJET_ID);
        }
    }

    @Nested
    @DisplayName("createBudgetProjet")
    class CreateBudgetProjet {

        @Test
        @DisplayName("Crée le projet budgétisé avec succès quand toutes les données sont fournies")
        void donneesCompletes_succes() {
            BudgetProjet budgetProjet = new BudgetProjet();
            budgetProjet.setNom("Projet Forfait");
            budgetProjet.setClient(client);
            budgetProjet.setBudgetInitial(BigDecimal.valueOf(50000));
            budgetProjet.setTjm(BigDecimal.valueOf(600));

            when(budgetProjetRepository.save(budgetProjet)).thenReturn(budgetProjet);

            BudgetProjet resultat = projetService.createBudgetProjet(budgetProjet);

            assertEquals(BigDecimal.valueOf(50000), resultat.getBudgetInitial());
            verify(budgetProjetRepository).save(budgetProjet);
        }

        @Test
        @DisplayName("Lève IllegalArgumentException si aucun client n'est fourni")
        void sansClient_lanceException() {
            BudgetProjet budgetProjet = new BudgetProjet();
            budgetProjet.setBudgetInitial(BigDecimal.valueOf(50000));
            budgetProjet.setTjm(BigDecimal.valueOf(600));

            assertThrows(IllegalArgumentException.class,
                    () -> projetService.createBudgetProjet(budgetProjet));
            verifyNoInteractions(budgetProjetRepository);
        }

        @Test
        @DisplayName("Lève IllegalArgumentException si le budget initial est manquant")
        void sansBudgetInitial_lanceException() {
            BudgetProjet budgetProjet = new BudgetProjet();
            budgetProjet.setClient(client);
            budgetProjet.setTjm(BigDecimal.valueOf(600));

            assertThrows(IllegalArgumentException.class,
                    () -> projetService.createBudgetProjet(budgetProjet));
            verifyNoInteractions(budgetProjetRepository);
        }

        @Test
        @DisplayName("Lève IllegalArgumentException si le TJM est manquant")
        void sansTjm_lanceException() {
            BudgetProjet budgetProjet = new BudgetProjet();
            budgetProjet.setClient(client);
            budgetProjet.setBudgetInitial(BigDecimal.valueOf(50000));

            assertThrows(IllegalArgumentException.class,
                    () -> projetService.createBudgetProjet(budgetProjet));
            verifyNoInteractions(budgetProjetRepository);
        }
    }

    @Nested
    @DisplayName("getBudgetProjetById / getAllBudgetProjets / getBudgetProjetsByClient")
    class ReadBudgetProjet {

        @Test
        @DisplayName("Retourne le projet budgétisé s'il existe")
        void budgetProjetExiste_retourneBudgetProjet() {
            BudgetProjet budgetProjet = new BudgetProjet();
            budgetProjet.setId(PROJET_ID);

            when(budgetProjetRepository.findById(PROJET_ID)).thenReturn(Optional.of(budgetProjet));

            Optional<BudgetProjet> resultat = projetService.getBudgetProjetById(PROJET_ID);

            assertTrue(resultat.isPresent());
        }

        @Test
        @DisplayName("Retourne tous les projets budgétisés")
        void retourneTousLesBudgetProjets() {
            when(budgetProjetRepository.findAll()).thenReturn(List.of(new BudgetProjet()));

            List<BudgetProjet> resultat = projetService.getAllBudgetProjets();

            assertEquals(1, resultat.size());
        }

        @Test
        @DisplayName("Retourne les projets budgétisés d'un client")
        void retourneBudgetProjetsDuClient() {
            when(budgetProjetRepository.findByClientId(CLIENT_ID)).thenReturn(List.of(new BudgetProjet()));

            List<BudgetProjet> resultat = projetService.getBudgetProjetsByClient(CLIENT_ID);

            assertEquals(1, resultat.size());
            verify(budgetProjetRepository).findByClientId(CLIENT_ID);
        }
    }

    @Nested
    @DisplayName("updateBudgetProjet")
    class UpdateBudgetProjet {

        @Test
        @DisplayName("Met à jour le budget initial et le TJM du projet existant")
        void budgetProjetExiste_metAJour() {
            BudgetProjet existant = new BudgetProjet();
            existant.setId(PROJET_ID);
            existant.setBudgetInitial(BigDecimal.valueOf(10000));
            existant.setTjm(BigDecimal.valueOf(400));

            when(budgetProjetRepository.findById(PROJET_ID)).thenReturn(Optional.of(existant));
            when(budgetProjetRepository.save(any(BudgetProjet.class))).thenAnswer(inv -> inv.getArgument(0));

            BudgetProjet resultat = projetService.updateBudgetProjet(
                    PROJET_ID, BigDecimal.valueOf(75000), BigDecimal.valueOf(650));

            assertEquals(BigDecimal.valueOf(75000), resultat.getBudgetInitial());
            assertEquals(BigDecimal.valueOf(650), resultat.getTjm());

            ArgumentCaptor<BudgetProjet> captor = ArgumentCaptor.forClass(BudgetProjet.class);
            verify(budgetProjetRepository).save(captor.capture());
            assertEquals(PROJET_ID, captor.getValue().getId());
        }

        @Test
        @DisplayName("Lève IllegalArgumentException si le projet budgétisé n'existe pas")
        void budgetProjetInexistant_lanceException() {
            when(budgetProjetRepository.findById(PROJET_ID)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> projetService.updateBudgetProjet(
                            PROJET_ID, BigDecimal.valueOf(75000), BigDecimal.valueOf(650)));
            verify(budgetProjetRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteBudgetProjet")
    class DeleteBudgetProjet {

        @Test
        @DisplayName("Supprime le projet budgétisé par son ID")
        void supprimeBudgetProjet() {
            projetService.deleteBudgetProjet(PROJET_ID);

            verify(budgetProjetRepository).deleteById(PROJET_ID);
        }
    }
}
