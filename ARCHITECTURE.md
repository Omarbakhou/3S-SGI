# Système de Gestion des Imputations (SGI) - Documentation Backend

## Vue d'ensemble

Le backend SGI est construit avec Spring Boot 4.1.0, JPA/Hibernate et PostgreSQL. Il implémente une architecture complète pour la gestion des imputations, des affectations et des collaborateurs avec des rôles différenciés.

## Architecture

### Hiérarchie des Entités

```
Collaborateur (classe mère)
├── Employe (peut saisir des imputations)
└── Manager (peut valider les imputations)
```

### Relations

- **Collaborateur ↔ Projet** : Relation Many-to-Many via Affectation (avec taux_affectation)
- **Employe ↔ Imputation** : Relation One-to-Many
- **Manager ↔ Imputation** : Relation One-to-Many (validation)
- **Projet ↔ Imputation** : Relation One-to-Many
- **Projet ↔ Client** : Relation Many-to-One
- **BudgetProjet** : Hérite de Projet (Joined Table Inheritance)

## Interfaces

### ImputationInterface
Définit le comportement d'une imputation :
- `getNomImputation()` / `setNomImputation()`
- `getNomProjet()`
- `getNomEmploye()`
- `getStatutImputation()` / `setStatutImputation()`

### ValidationInterface
Définit le comportement de validation :
- `valider(Manager manager)` - Valide l'imputation
- `rejeter(Manager manager)` - Rejette l'imputation
- `getManagerValidateur()`
- `getDateValidation()`

## Services

### CollaborateurService
Gère le CRUD et les opérations sur les comptes collaborateurs :
- **CRUD** : createEmploye(), createManager(), getCollaborateurById(), updateCollaborateurProfile()
- **Sécurité** : changePassword(), updateCollaborateurProfile()
- **Requêtes** : getEmployeById(), getManagerById()

**Droits d'accès** :
- Chaque collaborateur peut modifier son propre profil
- Chaque collaborateur peut changer son mot de passe
- Les administrateurs gèrent tous les comptes

### ImputationService
Gère le CRUD et la validation des imputations :
- **Employe CRUD** : createImputation(), updateImputation(), deleteImputation()
- **Manager Validation** : validerImputation(), rejeterImputation()
- **Requêtes** : getImputationsByEmploye(), getImputationsByProjet(), getImputationsByStatut()

**Droits d'accès** :
- Les employés peuvent créer, modifier et supprimer leurs propres imputations (en attente)
- Les managers peuvent valider ou rejeter les imputations en attente

### AffectationService
Gère les affectations (relation Many-to-Many) :
- **CRUD** : createAffectation(), updateTauxAffectation(), deleteAffectation()
- **Métier** : getTauxAffectationTotal(), canAffectCollaborateur()

**Droits d'accès** :
- Les managers décident du taux d'affectation (50% sur Projet A, 30% sur Projet B, etc.)
- Le taux d'affectation est une donnée métier, pas une donnée dérivée

### ProjetService
Gère les projets et les budgets :
- **Projets** : createProjet(), updateProjet(), getProjetsByClient()
- **Budget** : createBudgetProjet(), updateBudgetProjet(), getBudgetProjetsByClient()
- **Clients** : createClient(), updateClient()

**Droits d'accès** :
- Les managers et administrateurs gèrent les projets
- Les administrateurs créent et modifient les clients

## Contrôleurs

### CollaborateurController (`/api/collaborateurs`)
**Endpoints** :
- `POST /employe` - Créer un employé (ADMIN)
- `POST /manager` - Créer un manager (ADMIN)
- `GET /{id}` - Récupérer le profil (EMPLOYE, MANAGER)
- `PUT /{id}/profile` - Mettre à jour le profil (EMPLOYE, MANAGER)
- `POST /{id}/change-password` - Changer le mot de passe (EMPLOYE, MANAGER)
- `GET /employes` - Lister tous les employés (ADMIN)
- `GET /managers` - Lister tous les managers (ADMIN)

### ImputationController (`/api/imputations`)
**Endpoints** :
- `POST /` - Créer une imputation (EMPLOYE)
- `GET /{id}` - Récupérer une imputation (EMPLOYE, MANAGER)
- `GET /employe/{employeId}` - Imputations d'un employé (EMPLOYE, MANAGER)
- `GET /en-attente` - Imputations en attente (MANAGER)
- `PUT /{id}` - Mettre à jour (EMPLOYE)
- `DELETE /{id}` - Supprimer (EMPLOYE)
- `POST /{imputationId}/valider` - Valider (MANAGER)
- `POST /{imputationId}/rejeter` - Rejeter (MANAGER)

### AffectationController (`/api/affectations`)
**Endpoints** :
- `POST /` - Créer une affectation (MANAGER, ADMIN)
- `GET /{collaborateurId}/{projetId}` - Récupérer une affectation (EMPLOYE, MANAGER)
- `GET /collaborateur/{collaborateurId}` - Affectations d'un collaborateur (EMPLOYE, MANAGER, ADMIN)
- `GET /projet/{projetId}` - Affectations d'un projet (MANAGER, ADMIN)
- `PUT /{collaborateurId}/{projetId}/taux` - Mettre à jour le taux (MANAGER, ADMIN)
- `DELETE /{collaborateurId}/{projetId}` - Supprimer (MANAGER, ADMIN)
- `GET /{collaborateurId}/taux-total` - Taux total d'un collaborateur (EMPLOYE, MANAGER, ADMIN)

### ProjetController (`/api/projets`)
**Endpoints** :
- `POST /` - Créer un projet (MANAGER, ADMIN)
- `GET /{id}` - Récupérer un projet (EMPLOYE, MANAGER)
- `GET /client/{clientId}` - Projets d'un client (MANAGER, ADMIN)
- `POST /clients` - Créer un client (ADMIN)
- `GET /clients` - Lister les clients (MANAGER, ADMIN)
- `POST /budget` - Créer un projet avec budget (MANAGER, ADMIN)
- `GET /budget/{id}` - Récupérer un budget projet (MANAGER, ADMIN)

## Statuts des Imputations

- **EN_ATTENTE** : Imputation créée par l'employé, en attente de validation
- **VALIDEE** : Imputation validée par le manager
- **REJETEE** : Imputation rejetée par le manager

## Contrôles et Validations

### Affectation
- Le taux d'affectation doit être entre 0 et 100 (%)
- La somme des taux ne devrait idéalement pas dépasser 100% par collaborateur

### Imputation
- Seule une imputation en attente peut être modifiée ou supprimée par l'employé
- Une imputation en attente sans manager ne peut pas avoir de date de validation
- Une imputation validée/rejetée doit avoir un manager et une date de validation

## Configuration

### Variables d'environnement (application.properties)
```
spring.datasource.url=jdbc:postgresql://localhost:5432/sgi_db
spring.datasource.username=sgi_user
spring.datasource.password=sgi_password
spring.jpa.hibernate.ddl-auto=update
jwt.secret=mySecretKeyForJWTTokenGenerationAndValidationMustBeAtLeast256BitSGI2024
jwt.expiration=86400000
```

## DTOs (Data Transfer Objects)

- **CollaborateurDTO** : Données du collaborateur
- **ImputationDTO** : Données d'imputation
- **AffectationDTO** : Données d'affectation
- **ProjetDTO** : Données du projet
- **BudgetProjetDTO** : Données du projet budgété
- **ClientDTO** : Données du client

## Gestion des Erreurs

Les exceptions sont gérées globalement par `GlobalExceptionHandler` :
- `ValidationException` → HTTP 400
- `ResourceNotFoundException` → HTTP 404
- `IllegalArgumentException` → HTTP 400
- `IllegalStateException` → HTTP 409
- Autres exceptions → HTTP 500

## Sécurité

### Authentification
- HTTP Basic Authentication
- Password Encoder: BCrypt

### Autorisation
- Rôles : ADMIN, MANAGER, EMPLOYE
- Annotations : `@PreAuthorize("hasRole('ROLE')")`
- CORS configuré pour tous les origines

## Hypothèses de Conception

1. **Taux d'affectation** : Donnée métier, décidée par les managers, non calculable à partir d'autres données
2. **Héritage** : Stratégie JOINED pour Collaborateur (Employe/Manager) et Projet (BudgetProjet)
3. **Validation d'imputation** : Implémentée via l'interface `ValidationInterface` sur l'entité `Imputation`
4. **Sécurité** : Basée sur les rôles avec annotations `@PreAuthorize` au niveau des contrôleurs

## Démarrage de l'Application

```bash
# Créer la base de données PostgreSQL
createdb sgi_db

# Compiler et exécuter
mvn clean install
mvn spring-boot:run

# L'application sera disponible à : http://localhost:8080/api
```

## Prochaines Étapes

- Ajouter JWT Token-based Authentication
- Ajouter les tests unitaires et d'intégration
- Ajouter la documentation Swagger/OpenAPI
- Implémenter les filtres de recherche avancés
- Ajouter les audits de modification des données

