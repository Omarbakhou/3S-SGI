package com.SSS.SGI.service;

import com.SSS.SGI.entity.Collaborateur;
import com.SSS.SGI.exception.ResourceNotFoundException;
import com.SSS.SGI.repository.CollaborateurRepository;
import com.SSS.SGI.security.AdminEmails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Activation et désactivation des comptes, réservées aux administrateurs.
 *
 * <p>Convention du projet : on ne supprime jamais un compte physiquement. Désactiver
 * bascule {@code actif} à false ; les imputations, absences et affectations qui
 * référencent le collaborateur restent intactes, seul l'accès est retiré.
 */
@Service
public class AdminCompteService {

    private final CollaborateurRepository collaborateurRepository;
    private final AdminEmails adminEmails;

    public AdminCompteService(CollaborateurRepository collaborateurRepository, AdminEmails adminEmails) {
        this.collaborateurRepository = collaborateurRepository;
        this.adminEmails = adminEmails;
    }

    /**
     * Désactive un compte.
     *
     * @param cibleId          le compte à désactiver
     * @param adminId          l'administrateur qui agit, tel qu'issu du JWT (jamais de l'URL)
     * @throws ResourceNotFoundException si le compte cible n'existe pas
     * @throws IllegalStateException     si l'admin vise son propre compte, ou le dernier admin actif
     */
    @Transactional
    public Collaborateur desactiver(Long cibleId, Long adminId) {
        if (cibleId.equals(adminId)) {
            throw new IllegalStateException(
                    "Vous ne pouvez pas désactiver votre propre compte administrateur.");
        }

        Collaborateur cible = charger(cibleId);
        if (!cible.isActif()) {
            return cible;
        }

        if (estDernierAdministrateurActif(cible)) {
            throw new IllegalStateException(
                    "Impossible de désactiver le dernier compte administrateur actif : "
                            + "plus personne ne pourrait administrer le système.");
        }

        cible.setActif(false);
        return collaborateurRepository.save(cible);
    }

    /** Réactive un compte précédemment désactivé. Sans effet s'il est déjà actif. */
    @Transactional
    public Collaborateur reactiver(Long cibleId) {
        Collaborateur cible = charger(cibleId);
        if (cible.isActif()) {
            return cible;
        }
        cible.setActif(true);
        return collaborateurRepository.save(cible);
    }

    private Collaborateur charger(Long id) {
        return collaborateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun collaborateur avec l'identifiant " + id));
    }

    /**
     * Le rôle ADMIN n'existe pas en base : il vient de la liste ADMIN_EMAILS. Le dernier
     * administrateur actif est donc le seul compte encore actif dont l'email y figure.
     */
    private boolean estDernierAdministrateurActif(Collaborateur cible) {
        if (!adminEmails.contient(cible.getEmail())) {
            return false;
        }
        List<Collaborateur> administrateursActifs = collaborateurRepository.findAll().stream()
                .filter(Collaborateur::isActif)
                .filter(c -> adminEmails.contient(c.getEmail()))
                .toList();
        return administrateursActifs.size() <= 1;
    }
}
