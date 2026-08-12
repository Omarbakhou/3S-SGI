# État de la sécurité — SGI Backend

_Dernière mise à jour : 2026-08-12 — activation de l'authentification JWT + RBAC._

## Résumé

L'authentification par JWT et le RBAC (`ADMIN`, `MANAGER`, `EMPLOYE`) sont maintenant actifs
sur tous les endpoints `/api/**`, à l'exception de `/api/auth/login` et `/api/auth/refresh`.
Ce document décrit l'implémentation en place, comment obtenir un jeton, la limite structurelle
assumée sur le rôle `ADMIN`, et ce qui reste hors périmètre.

## Ce qui est en place

- **`SecurityConfig`** : `@EnableMethodSecurity(prePostEnabled = true)` — les `@PreAuthorize`
  déjà présents sur les contrôleurs (`AbsenceController`, `AffectationController`,
  `ClientController`, `CollaborateurController`, `ImputationController`, `ProjetController`)
  sont désormais évalués. Session stateless, `JwtAuthenticationFilter` inséré avant
  `UsernamePasswordAuthenticationFilter`. Toute requête sans authentification valide sur un
  endpoint protégé reçoit 401 ; un rôle insuffisant reçoit 403 (réponses JSON explicites, voir
  `SecurityConfig.writeError` pour le filtre et `GlobalExceptionHandler` pour les
  `AccessDeniedException`/`AuthenticationException` levées pendant le dispatch MVC — les deux
  chemins existent car une `AccessDeniedException` levée par `@PreAuthorize` est interceptée par
  le `@ControllerAdvice` avant d'atteindre le filtre de sécurité).
- **`JwtUtil`** : génère et valide les jetons (HS256, `jjwt` 0.12.3), claims `id` et `roles` en
  plus du `sub` (email). Durée de vie configurée par `jwt.expiration` (`application.properties`).
- **`CustomUserDetailsService`** : charge un `Collaborateur` par email et dérive son rôle depuis
  son type concret (`Employe` → `EMPLOYE`, `Manager` → `MANAGER`).
- **`JwtAuthenticationFilter`** : lit le header `Authorization: Bearer {token}`, peuple le
  `SecurityContext` à partir des claims du jeton (pas d'accès base à chaque requête). Un jeton
  absent ou invalide n'est jamais rejeté par le filtre lui-même — c'est la règle d'autorisation
  qui décide 401/403.
- **`AuthController`** : `POST /api/auth/login` (retourne le jeton + rôles), `POST
  /api/auth/refresh` (réémet un jeton à partir d'un jeton encore valide), `GET /api/auth/me`.

## Lancer l'application en local

Spring Boot ne charge pas `.env` automatiquement. Il faut exporter les variables avant de lancer
l'appli (`.mvnw spring-boot:run` ou équivalent IDE), sinon `${DB_PASSWORD}`/`${JWT_SECRET}` ne
résolvent à rien :

```powershell
$env:DB_PASSWORD = "..."
$env:JWT_SECRET = "..."
$env:ADMIN_EMAILS = "vous@exemple.com"
./mvnw spring-boot:run
```

Piège vérifié le 2026-08-12 : un ancien processus resté sur le port 8081 (build compilé avant
l'ajout du filtre JWT) causait un 500 générique sur `/api/auth/login`, alors que le nouveau code
fonctionnait déjà correctement — le nouveau build refusait de démarrer tant que ce processus
occupait le port. Si le port est déjà pris, tuer l'ancien processus (`Get-NetTCPConnection
-LocalPort 8081`) avant de relancer plutôt que de suspecter le code.

## Rôle ADMIN : pas d'entité dédiée

Contrainte du projet : pas de changement de schéma de base de données. Il n'existe donc pas de
table/entité `Admin`. Un manager est promu `ADMIN` (en plus de `ROLE_MANAGER`) si son email
figure dans la propriété `sgi.security.admin-emails` (liste séparée par des virgules), lue depuis
la variable d'environnement `ADMIN_EMAILS` (voir `.env`, non commité). C'est un choix explicite :
plus simple qu'une entité dédiée, mais un manager perd son statut ADMIN si son email est retiré
de la liste — il n'y a pas de trace en base de qui est admin.

## Correctif appliqué : IDOR sur `/profile` (mis à jour)

`PUT /api/collaborateurs/{id}/profile` exige maintenant que l'`id` du principal authentifié (JWT)
corresponde à l'`id` ciblé dans l'URL — sinon 403, avant même de vérifier le mot de passe. La
vérification `motDePasseActuel` déjà en place est conservée en défense en profondeur.

**Vérifié** (voir `RBACIntegrationTest`) :

| Scénario | Résultat |
|---|---|
| Employé modifie son propre profil (bon mot de passe) | 200 OK |
| Employé modifie le profil d'un autre collaborateur | 403 Forbidden |

## Hors périmètre de cette passe (gaps connus, non corrigés)

- **Ownership par principal non généralisé** : `POST /api/absences/employe/{employeId}`,
  `PUT/DELETE /api/imputations/{id}?employeId=...`, etc. acceptent toujours un `employeId`/
  `managerId` fourni par le client sans vérifier qu'il correspond au principal authentifié. Un
  EMPLOYE authentifié peut donc agir sur les données d'un autre `employeId` tant qu'il connaît
  son identifiant. Seul `/collaborateurs/{id}/profile` a été corrigé (demande explicite).
  Généraliser ce correctif (comparer systématiquement le principal JWT à l'id métier ciblé) reste
  à faire.
- **`POST /api/collaborateurs/{id}/change-password`** n'a pas reçu la même vérification de
  propriété que `/profile` — même classe de problème, non demandée dans cette passe.
- **Le hash du mot de passe est toujours renvoyé dans les réponses JSON** des endpoints
  collaborateur (pas de `@JsonIgnore` sur `motDePasse`).
- **`ADMIN` par liste d'emails plutôt que par entité** (voir ci-dessus) — acceptable tant que le
  nombre d'admins reste faible et géré via l'environnement de déploiement.

## Tests

- `JwtUtilTest` — génération/validation, expiration, signature invalide, altération.
- `SecurityIntegrationTest` — login (succès/échec), `/me`, `/refresh`, rejet 401 sans jeton ou
  jeton invalide.
- `RBACIntegrationTest` — application réelle des rôles sur `GET /api/collaborateurs`
  (`MANAGER`), `POST /api/absences/quotas` (`ADMIN`), `DELETE /api/collaborateurs/{id}`
  (`MANAGER`), et le correctif IDOR sur `/profile`.

`./mvnw test` — 70/70 tests passent (25 nouveaux, 45 préexistants inchangés).
