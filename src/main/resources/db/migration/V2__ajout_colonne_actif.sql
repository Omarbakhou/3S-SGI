-- =====================================================================
-- V2 — Désactivation logique des comptes
--
-- Convention du projet : on ne supprime jamais physiquement un compte,
-- sinon l'historique des imputations et des absences qui le référencent
-- serait perdu. Un compte retiré est marqué `actif = false` : ses lignes
-- liées restent intactes, mais il ne peut plus se connecter.
--
-- La colonne est portée par `collaborateur` (table de base de la
-- hiérarchie JOINED), elle couvre donc à la fois les managers et les
-- employés sans duplication.
-- =====================================================================

ALTER TABLE public.collaborateur
    ADD COLUMN actif boolean NOT NULL DEFAULT true;

COMMENT ON COLUMN public.collaborateur.actif IS
    'false = compte désactivé par un administrateur : connexion refusée, données historiques conservées.';
