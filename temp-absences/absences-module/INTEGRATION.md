# Intégration du module Absences/Maladies dans 3S-SGI

## 1. Copier les fichiers
Copiez le contenu de `src/main/java/com/SSS/SGI/` dans votre arborescence
existante (même package racine `com.SSS.SGI`). Aucun fichier existant n'est
écrasé : tout est additif (nouvelles classes uniquement).

## 2. GlobalExceptionHandler
Ajoutez ces trois mappings à votre `GlobalExceptionHandler.java` existant,
sur le même modèle que `ValidationException` (400 Bad Request) :

```java
@ExceptionHandler(QuotaInsuffisantException.class)
public ResponseEntity<ErrorResponse> handleQuotaInsuffisant(QuotaInsuffisantException ex, HttpServletRequest req) {
    return buildErrorResponse(HttpStatus.BAD_REQUEST, "Quota insuffisant", ex.getMessage(), req);
}

@ExceptionHandler(AbsenceChevauchementException.class)
public ResponseEntity<ErrorResponse> handleChevauchement(AbsenceChevauchementException ex, HttpServletRequest req) {
    return buildErrorResponse(HttpStatus.BAD_REQUEST, "Chevauchement d'absence", ex.getMessage(), req);
}

@ExceptionHandler(JustificatifManquantException.class)
public ResponseEntity<ErrorResponse> handleJustificatifManquant(JustificatifManquantException ex, HttpServletRequest req) {
    return buildErrorResponse(HttpStatus.BAD_REQUEST, "Justificatif manquant", ex.getMessage(), req);
}
```
(Adaptez `buildErrorResponse` au helper déjà utilisé pour vos autres handlers.)

## 3. SecurityConfig
Ajoutez les routes `/api/absences/**` à votre configuration `SecurityConfig.java`
avec les mêmes règles de rôle que le contrôleur (actuellement tout est
`permitAll()` en dev selon votre doc — pensez à les activer en même temps
que le reste quand vous réactiverez la sécurité).

## 4. application.properties
```properties
# Dossier de stockage des justificatifs (maladie, maternité/paternité)
sgi.fichiers.dossier-justificatifs=./justificatifs
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

## 5. Base de données
Avec `spring.jpa.hibernate.ddl-auto=update`, les tables `absence` et
`quota_absence` seront créées automatiquement au démarrage. Schéma généré :

- `absence` : id_absence [PK], type_absence, date_debut, date_fin,
  nombre_jours, statut, id_employe [FK], id_manager_validateur [FK,
  nullable], date_demande, date_validation (nullable), commentaire_employe,
  motif_rejet, justificatif_url, date_envoi_justificatif
- `quota_absence` : id_quota [PK], id_employe [FK], type_absence, annee,
  jours_alloues, jours_pris — contrainte unique (id_employe, type_absence,
  annee)

## 6. Points à valider avec vous
- **MALADIE et CONGE_MATERNITE_PATERNITE** ne décomptent aucun quota dans
  cette version (règle FR usuelle : les arrêts maladie ne s'imputent pas
  sur les congés payés). Si votre besoin diffère, il suffit de basculer
  `soumisAQuota` à `true` dans `TypeAbsence`.
- **Jours fériés** : le calcul actuel exclut seulement samedi/dimanche.
  Si nécessaire, on ajoute une table `JourFerie` et on l'intègre au calcul.
- **Stockage des justificatifs** : implémentation disque simple pour
  démarrer ; à remplacer par un stockage objet (S3, Azure Blob) si vous
  déployez en production.

## 7. Cohérence avec le DCG
Ce module introduit deux tables et deux enums non présents dans le DCG
actuel (section 8). Si vous voulez, je peux mettre à jour le document Word
pour les documenter (MLD, règles de gestion, section Sécurité).
