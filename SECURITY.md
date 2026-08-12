# État de la sécurité — SGI Backend

_Dernière vérification manuelle : 2026-08-12_

## Résumé

**L'authentification est désactivée.** Tous les endpoints `/api/**` sont actuellement
accessibles sans identifiant, quel que soit le rôle annoté sur le contrôleur. Ce
document décrit précisément ce qui est en place, ce qui ne l'est pas, et le correctif
ponctuel appliqué en attendant qu'une vraie couche d'authentification soit construite.

## Constat vérifié manuellement (curl, 2026-08-12)

Application démarrée localement (`./mvnw spring-boot:run`, port 8081, PostgreSQL local) :

| Requête | Annotation `@PreAuthorize` | Credentials fournis | Résultat observé |
|---|---|---|---|
| `GET /api/collaborateurs` | `hasRole('MANAGER')` | Aucun | **200 OK** |
| `GET /api/absences/en-attente` | `hasRole('MANAGER')` | Aucun | **200 OK** |
| `GET /api/imputations` | `hasRole('MANAGER')` | Aucun | **200 OK** |
| `DELETE /api/collaborateurs/{id}` | `hasRole('MANAGER')` | Aucun | **200 OK — suppression effectuée** |

Les annotations `@PreAuthorize` présentes sur `AbsenceController`, `AffectationController`,
`ClientController`, `CollaborateurController` et `ImputationController` référencent des
rôles (`ADMIN`, `MANAGER`, `EMPLOYE`) qui **n'existent pas** en tant qu'autorités Spring
Security dans ce projet — il n'y a ni entité `Admin`, ni enum `Role`, ni
`UserDetailsService`, ni endpoint de login, ni filtre JWT. Ces annotations ne font
actuellement rien, pour deux raisons cumulatives :

1. `SecurityConfig.java` déclare `@EnableMethodSecurity(prePostEnabled = false)` —
   `@PreAuthorize` n'est pas évalué du tout.
2. La chaîne de filtres autorise tout (`.anyRequest().permitAll()`) et il n'existe
   aucun filtre qui peuplerait un `Authentication` avec des autorités de rôle de toute
   façon.

`SecurityConfig.java` documente lui-même cet état comme temporaire :

```java
// Authentification désactivée temporairement : tous les endpoints /api/** sont ouverts.
```

## Correctif appliqué : IDOR sur `/profile`

`PUT /api/collaborateurs/{id}/profile` acceptait `id` depuis l'URL sans aucune
vérification — n'importe qui pouvait modifier le nom/prénom/email de n'importe quel
compte en devinant un ID. C'était un IDOR aveugle (aucune preuve de propriété requise).

À titre de comparaison, `POST /api/collaborateurs/{id}/change-password` exigeait déjà
`ancienMotDePasse` et refusait la modification si le mot de passe actuel ne
correspondait pas à celui du compte ciblé — ce n'est pas une vraie authentification,
mais ça empêche un attaquant sans information de modifier un compte à l'aveugle.

**Correctif** : `updateProfile` exige maintenant `motDePasseActuel` dans le corps de la
requête et le vérifie contre le mot de passe du compte ciblé avant toute modification,
suivant exactement le même principe que `change-password`.

- `UpdateProfileRequest.motDePasseActuel` (nouveau champ)
- `CollaborateurService.updateCollaborateurProfile(id, nom, prenom, email, motDePasseActuel)`
  lève `IllegalArgumentException` (→ 400) si le mot de passe ne correspond pas.

Vérifié manuellement :

| Scénario | Résultat |
|---|---|
| Modifier le profil de l'id=3 sans `motDePasseActuel` | 400 `Le mot de passe actuel est incorrect` |
| Modifier le profil de l'id=3 avec un mot de passe erroné | 400 `Le mot de passe actuel est incorrect` |
| Modifier le profil de l'id=3 avec le bon mot de passe | 200 OK |

**Limite assumée** : ceci n'est **pas** un contrôle d'autorisation réel — sans session ni
JWT, il n'y a aucun moyen de savoir qui appelle l'API. Le mot de passe actuel sert de
preuve de propriété minimale, pas d'authentification. Le vrai correctif consiste à
comparer l'`id` du principal authentifié à l'`id` cible une fois qu'une couche
d'authentification existera (voir ci-dessous), et à retirer/assouplir alors cette
vérification par mot de passe si elle devient redondante.

## Autres constats (non corrigés, hors périmètre de cette passe)

- **Secrets en clair dans `application.properties`** : le mot de passe PostgreSQL
  (`spring.datasource.password`) est en clair et commité dans le dépôt. À déplacer vers
  une variable d'environnement avant tout déploiement partagé.
- **`jwt.secret` / `jwt.expiration`** sont définis dans `application.properties` et
  `application-test.properties` mais ne sont utilisés par aucun code — reliquat d'une
  configuration JWT jamais branchée dans ce dépôt.
- **Le hash du mot de passe (`motDePasse`) est renvoyé dans les réponses JSON** des
  endpoints collaborateur (`Collaborateur`/`Employe`/`Manager` n'ont pas de
  `@JsonIgnore` sur ce champ). Le hash BCrypt n'est pas trivialement réversible, mais
  ne devrait pas transiter côté client.

## Pour aller plus loin

Une implémentation JWT + RBAC + correctifs IDOR par principal authentifié existe déjà
(dans un état non intégré à ce dépôt) et peut servir de base si/quand l'authentification
doit être réactivée : `JwtUtil`, `JwtAuthenticationFilter`, `CustomUserDetailsService`,
`SecurityService`, un enum `Role` et une entité `Admin`. Ce travail n'a pas été porté ici
sur décision explicite — le périmètre de cette passe s'est limité à documenter l'état
réel et à fermer l'IDOR le plus flagrant.