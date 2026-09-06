# 3S-SGI — Système de Gestion des Imputations

Application de suivi des imputations de temps, des absences et des affectations
projet. Backend Spring Boot + PostgreSQL, frontend React/Vite.

Trois rôles : `ADMIN`, `MANAGER`, `EMPLOYE`. Il n'existe **pas de table Admin** en
base : le rôle `ADMIN` est accordé au démarrage à un manager dont l'email figure
dans la variable d'environnement `ADMIN_EMAILS`.

- Architecture détaillée : [ARCHITECTURE.md](ARCHITECTURE.md)
- Historique et décisions de sécurité : [SECURITY.md](SECURITY.md)

## Prérequis

| Outil | Version |
|---|---|
| JDK | 17+ |
| PostgreSQL | 16 |
| Node.js | 18+ |

Maven n'a pas besoin d'être installé : utilisez le wrapper `./mvnw`
(`.\mvnw.cmd` sous PowerShell).

## Premier démarrage

### 1. Variables d'environnement

Copiez `.env.example` en `.env` à la racine et renseignez vos valeurs :

```bash
cp .env.example .env
```

`.env` est ignoré par git — aucun secret ne doit être versionné.

| Variable | Rôle |
|---|---|
| `DB_PASSWORD` | Mot de passe du rôle PostgreSQL |
| `JWT_SECRET` | Clé de signature des JWT, 256 bits minimum (`openssl rand -base64 48`) |
| `ADMIN_EMAILS` | Emails promus `ADMIN`, séparés par des virgules |

### 2. Base de données

Créez une base PostgreSQL accessible sur `localhost:5432`. **Ne créez aucune
table à la main** : le schéma est intégralement géré par Flyway
(`src/main/resources/db/migration/`) et appliqué au démarrage.

- `V1__baseline.sql` — schéma de référence, permet de repartir d'une base vierge
- `V2__ajout_colonne_actif.sql` — désactivation logique des comptes
- `V3__compte_admin_initial.sql` — retrait des comptes de test, création de l'admin

Sur une base déjà créée par l'ancien `ddl-auto=update`, Flyway marque
automatiquement `V1` comme appliquée (baseline) et ne joue que les migrations
suivantes. Hibernate est en `ddl-auto=validate` : il vérifie que le schéma
correspond aux entités mais ne le modifie plus. **Toute évolution du schéma passe
désormais par une nouvelle migration `Vn__description.sql`.**

### 3. Lancer le backend

```powershell
.\run.ps1          # charge .env puis démarre Spring Boot sur le port 8081
```

Au **tout premier démarrage**, la migration `V3` crée le compte administrateur
`admin@3s-sgi.local` sans mot de passe utilisable, et l'application en génère un
aléatoire qu'elle affiche **une seule fois** dans les logs :

```
==========================================================================
 COMPTE ADMINISTRATEUR INITIALISÉ
 Email        : admin@3s-sgi.local
 Mot de passe : <généré aléatoirement>

 Ce mot de passe est affiché UNE SEULE FOIS. Notez-le, connectez-vous,
 puis changez-le depuis votre profil.
==========================================================================
```

Notez-le, connectez-vous, puis changez-le. Les démarrages suivants ne réaffichent
rien et ne réinitialisent pas le mot de passe.

> L'email de ce compte doit figurer dans `ADMIN_EMAILS`, sinon il existe mais
> reste un simple manager et la page Administration lui est fermée. L'application
> le signale par un avertissement au démarrage.

### 4. Lancer le frontend

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173
```

Le serveur de dev relaie `/api` vers le backend : le navigateur ne parle qu'à une
seule origine, il n'y a donc pas de CORS à gérer en développement.

## Gestion des comptes

Seul un `ADMIN` peut créer un compte, depuis la page **Administration**. Il n'y a
pas d'inscription publique : aucun endpoint `/register` ou `/signup` n'existe, et
`POST /api/collaborateurs/employe` comme `POST /api/collaborateurs/manager` sont
protégés côté backend par `@PreAuthorize("hasRole('ADMIN')")` — pas seulement
masqués dans l'interface.

### Désactivation plutôt que suppression

**Un compte n'est jamais supprimé physiquement.** Supprimer la ligne emporterait
l'historique des imputations et des absences qui la référencent. Retirer un accès
se fait par désactivation, depuis la colonne *Statut* du tableau « Comptes
existants » :

| Action | Endpoint |
|---|---|
| Désactiver | `PATCH /api/admin/comptes/{id}/desactiver` |
| Réactiver | `PATCH /api/admin/comptes/{id}/reactiver` |

Un compte désactivé ne peut plus se connecter (message explicite, pas une erreur
générique), et ses jetons déjà émis cessent immédiatement de fonctionner — le
filtre JWT revérifie l'état du compte à chaque requête, sans quoi un accès retiré
resterait valable jusqu'à l'expiration du jeton (24 h).

Deux garde-fous refusent l'opération (`409 Conflict`) :

- un administrateur ne peut pas désactiver **son propre compte** ;
- on ne peut pas désactiver le **dernier administrateur actif**, sinon plus
  personne ne pourrait administrer le système.

## Nom de domaine local (`sgi.local`)

Pour afficher `http://sgi.local:5173` plutôt que `localhost` — utile lors d'une
démonstration enregistrée.

> Configuration **locale, à faire sur chaque poste**. Le fichier `hosts` n'est pas
> versionnable : ce n'est pas une configuration de déploiement.

### Windows

Ouvrez un éditeur **en tant qu'administrateur** (droits requis), puis ajoutez
cette ligne à `C:\Windows\System32\drivers\etc\hosts` :

```
127.0.0.1  sgi.local
```

Vérifiez ensuite :

```powershell
ping sgi.local          # doit répondre depuis 127.0.0.1
```

### Ce qui est déjà configuré côté projet

- `frontend/vite.config.js` : `host: true` (Vite écoute sur toutes les interfaces,
  sinon il ne répond qu'à `127.0.0.1`) et `allowedHosts: ['sgi.local', 'localhost']`
  (sans quoi Vite rejette la requête avec « Blocked request »).
- `SecurityConfig` : `http://sgi.local:5173` **et** `http://localhost:5173` sont
  tous deux autorisés en CORS. Les deux sont conservés volontairement pour ne pas
  casser le poste de qui n'a pas modifié son fichier `hosts`.

L'application reste donc accessible aux deux adresses.

### HTTPS local (optionnel, non implémenté)

Servir en HTTPS avec [`mkcert`](https://github.com/FiloSottile/mkcert)
supprimerait le bandeau « Non sécurisé » du navigateur pendant un enregistrement.
Non mis en place ici : cela impose de gérer un certificat pour le proxy Vite en
plus du backend, pour un gain purement cosmétique.

## Tests

```bash
./mvnw test
```

172 tests (unitaires + intégration sécurité). Le profil `test` utilise une base
H2 en mémoire ; Flyway y est désactivé (les migrations sont écrites pour
PostgreSQL) et c'est Hibernate qui crée le schéma depuis les entités.

Le frontend n'a pas de suite de tests ; sa vérification est la compilation :

```bash
cd frontend && npm run build
```

## Conventions du projet

- Le champ mot de passe dans les payloads est `motDePasse` (jamais `password`).
- Les collaborateurs sont exposés sous `/api/collaborateurs` (pas `/api/employes`).
- Le hash de mot de passe est protégé par `@JsonProperty(access = WRITE_ONLY)` :
  il entre mais ne ressort jamais.
- **L'identifiant utilisé pour une action sensible vient toujours du JWT**, jamais
  d'un paramètre d'URL — voir `IdentiteAppelant`. Un id dans l'URL est une
  assertion du client à confronter au jeton, pas une preuve d'identité.
- Toute suppression métier est une désactivation logique, jamais un `DELETE`.
