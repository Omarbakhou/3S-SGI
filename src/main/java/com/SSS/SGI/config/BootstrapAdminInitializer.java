package com.SSS.SGI.config;

import com.SSS.SGI.entity.Collaborateur;
import com.SSS.SGI.repository.CollaborateurRepository;
import com.SSS.SGI.security.AdminEmails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

/**
 * Donne un mot de passe utilisable au compte administrateur créé par la migration V3.
 *
 * <p>La migration insère ce compte avec une sentinelle en guise de hash : aucun mot de
 * passe versionné dans git, donc rien à changer si le dépôt est partagé ou rendu public.
 * Au premier démarrage, ce composant repère la sentinelle, tire un mot de passe
 * aléatoire, l'enregistre haché et l'affiche une seule fois dans les logs.
 *
 * <p>Les démarrages suivants ne font rien : le hash est alors un vrai BCrypt, et le mot
 * de passe n'est jamais réaffiché. Un administrateur qui aurait perdu le sien repasse
 * par une réinitialisation, pas par ce composant.
 */
@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

    /** Doit correspondre à la valeur insérée par V3__compte_admin_initial.sql. */
    private static final String SENTINELLE = "!bootstrap";

    private final CollaborateurRepository collaborateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminEmails adminEmails;

    public BootstrapAdminInitializer(
            CollaborateurRepository collaborateurRepository,
            PasswordEncoder passwordEncoder,
            AdminEmails adminEmails) {
        this.collaborateurRepository = collaborateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmails = adminEmails;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Collaborateur> aInitialiser = collaborateurRepository.findAll().stream()
                .filter(c -> c.getMotDePasse() != null && c.getMotDePasse().startsWith(SENTINELLE))
                .toList();

        for (Collaborateur compte : aInitialiser) {
            String motDePasse = genererMotDePasse();
            compte.setMotDePasse(passwordEncoder.encode(motDePasse));
            collaborateurRepository.save(compte);
            annoncer(compte.getEmail(), motDePasse);
        }

        avertirSiAucunAdministrateurActif();
    }

    /** 24 octets aléatoires en Base64 URL-safe : assez long pour ne pas être deviné. */
    private String genererMotDePasse() {
        byte[] octets = new byte[24];
        new SecureRandom().nextBytes(octets);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
    }

    private void annoncer(String email, String motDePasse) {
        log.warn("""

                ==========================================================================
                 COMPTE ADMINISTRATEUR INITIALISÉ
                 Email        : {}
                 Mot de passe : {}

                 Ce mot de passe est affiché UNE SEULE FOIS. Notez-le, connectez-vous,
                 puis changez-le depuis votre profil.
                ==========================================================================
                """, email, motDePasse);
    }

    /**
     * Un compte administrateur dont l'email n'est pas dans ADMIN_EMAILS reste un simple
     * manager : la page Administration lui est fermée. Le cas est silencieux et difficile
     * à diagnostiquer, on le signale donc explicitement au démarrage.
     */
    private void avertirSiAucunAdministrateurActif() {
        if (adminEmails.emails().isEmpty()) {
            log.warn("ADMIN_EMAILS est vide : aucun compte n'obtiendra le rôle ADMIN, "
                    + "la page Administration sera inaccessible.");
            return;
        }
        boolean auMoinsUn = collaborateurRepository.findAll().stream()
                .anyMatch(c -> c.isActif() && adminEmails.contient(c.getEmail()));
        if (!auMoinsUn) {
            log.warn("Aucun compte actif ne correspond à ADMIN_EMAILS ({}) : "
                    + "personne ne peut administrer le système.", adminEmails.emails());
        }
    }
}
