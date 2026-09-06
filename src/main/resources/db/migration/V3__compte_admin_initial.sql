-- =====================================================================
-- V3 — Retrait des comptes de test et création du compte administrateur
--
-- Les comptes de bootstrap historiques (test.admin, test.manager,
-- test.employe, runskill.temp…) avaient des mots de passe connus et
-- traînaient dans le tableau « Comptes existants » de la page
-- Administration. Ils sont retirés ici, et remplacés par un unique
-- compte administrateur réel.
--
-- Ce compte est créé SANS mot de passe utilisable : la colonne porte une
-- sentinelle qui ne peut correspondre à aucun hash BCrypt (tous commencent
-- par « $2 »). Au premier démarrage, BootstrapAdminInitializer détecte
-- cette sentinelle, tire un mot de passe aléatoire et l'affiche une seule
-- fois dans les logs. Aucun secret n'est donc versionné dans git.
--
-- L'email doit correspondre à la variable d'environnement ADMIN_EMAILS,
-- sans quoi le compte existe mais n'obtient pas le rôle ADMIN.
-- Voir la section « Premier démarrage » du README.
-- =====================================================================

-- 1. Comptes de test à retirer, avec les lignes qui les référencent.
CREATE TEMPORARY TABLE comptes_de_test_a_supprimer ON COMMIT DROP AS
SELECT id_collaborateur
FROM public.collaborateur
WHERE lower(email) IN (
    'test.admin@example.com',
    'test.manager@example.com',
    'test.employe@example.com',
    'runskill.temp@example.com',
    'cible.demo@example.com'
);

-- Un compte supprimé ne doit pas emporter les données d'un compte conservé :
-- on se contente de retirer son rôle de validateur sur celles-ci.
UPDATE public.imputation
SET id_manager_validateur = NULL
WHERE id_manager_validateur IN (SELECT id_collaborateur FROM comptes_de_test_a_supprimer);

UPDATE public.absence
SET id_manager_validateur = NULL
WHERE id_manager_validateur IN (SELECT id_collaborateur FROM comptes_de_test_a_supprimer);

UPDATE public.employe
SET id_manager = NULL
WHERE id_manager IN (SELECT id_collaborateur FROM comptes_de_test_a_supprimer);

-- Données propres aux comptes de test : elles disparaissent avec eux.
DELETE FROM public.absence
WHERE id_employe IN (SELECT id_collaborateur FROM comptes_de_test_a_supprimer);

DELETE FROM public.imputation
WHERE id_employe IN (SELECT id_collaborateur FROM comptes_de_test_a_supprimer);

DELETE FROM public.quota_absence
WHERE id_employe IN (SELECT id_collaborateur FROM comptes_de_test_a_supprimer);

DELETE FROM public.affectation
WHERE id_collaborateur IN (SELECT id_collaborateur FROM comptes_de_test_a_supprimer);

DELETE FROM public.employe
WHERE id_collaborateur IN (SELECT id_collaborateur FROM comptes_de_test_a_supprimer);

DELETE FROM public.manager
WHERE id_collaborateur IN (SELECT id_collaborateur FROM comptes_de_test_a_supprimer);

DELETE FROM public.collaborateur
WHERE id_collaborateur IN (SELECT id_collaborateur FROM comptes_de_test_a_supprimer);

-- 2. Compte administrateur initial, idempotent : rejouer la migration sur une
--    base qui le possède déjà ne réinitialise pas son mot de passe.
INSERT INTO public.collaborateur (type_collaborateur, nom, prenom, email, mot_de_passe, actif)
SELECT 'MANAGER', 'Administrateur', 'SGI', 'admin@3s-sgi.local', '!bootstrap-mot-de-passe-a-generer', true
WHERE NOT EXISTS (
    SELECT 1 FROM public.collaborateur WHERE lower(email) = 'admin@3s-sgi.local'
);

INSERT INTO public.manager (id_collaborateur)
SELECT c.id_collaborateur
FROM public.collaborateur c
WHERE lower(c.email) = 'admin@3s-sgi.local'
  AND NOT EXISTS (
    SELECT 1 FROM public.manager m WHERE m.id_collaborateur = c.id_collaborateur
);
