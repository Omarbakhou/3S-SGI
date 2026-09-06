<#
.SYNOPSIS
    Recharge un jeu de données de démonstration cohérent dans 3S-SGI.

.DESCRIPTION
    Le script passe par l'API REST, pas par la base : il crée donc les données
    exactement comme le ferait un utilisateur, et échoue si une règle métier ou
    un contrôle de rôle est cassé. C'est à la fois un jeu de données et une
    vérification du parcours complet.

    Les comptes créés ici ont des mots de passe connus : ce sont des comptes de
    DÉMONSTRATION. Ne pas jouer ce script sur un environnement réel.

    Le quota de congés est calibré pour qu'une demande de 5 jours soit refusée
    pendant la démo (5 jours alloués, 3 déjà pris et validés, 2 restants).

.PARAMETER MotDePasseAdmin
    Mot de passe du compte admin@3s-sgi.local, affiché dans les logs au premier
    démarrage de l'application.

.PARAMETER UrlBase
    Racine de l'API. Par défaut http://localhost:8081

.EXAMPLE
    .\scripts\donnees-demo.ps1 -MotDePasseAdmin 'le-mot-de-passe-affiche-au-demarrage'
#>
param(
    [Parameter(Mandatory = $true)][string]$MotDePasseAdmin,
    [string]$UrlBase = 'http://localhost:8081'
)

$ErrorActionPreference = 'Stop'

# --- Comptes de démonstration -------------------------------------------------
$MotDePasseDemo = 'Demo2026!'
$EmailManager   = 'claire.durand@3s-sgi.local'
$EmailEmploye1  = 'karim.benali@3s-sgi.local'
$EmailEmploye2  = 'lucie.marchand@3s-sgi.local'
$Annee          = (Get-Date).Year

function Invoke-Api {
    param(
        [string]$Methode,
        [string]$Chemin,
        [string]$Jeton,
        $Corps
    )
    $entetes = @{}
    if ($Jeton) { $entetes['Authorization'] = "Bearer $Jeton" }

    $parametres = @{
        Uri     = "$UrlBase$Chemin"
        Method  = $Methode
        Headers = $entetes
    }

    if ($null -ne $Corps) {
        # Encodage explicite : sans cela les accents des noms passent en Latin-1.
        $json = $Corps | ConvertTo-Json -Depth 6 -Compress
        $parametres['Body'] = [System.Text.Encoding]::UTF8.GetBytes($json)
        $parametres['ContentType'] = 'application/json; charset=utf-8'
    }
    return Invoke-RestMethod @parametres
}

function Connexion {
    param([string]$Email, [string]$MotDePasse)
    $reponse = Invoke-Api -Methode POST -Chemin '/api/auth/login' -Corps @{ email = $Email; motDePasse = $MotDePasse }
    return $reponse.token
}

function Etape { param([string]$Texte) Write-Host "`n== $Texte" -ForegroundColor Cyan }

# --- 1. Connexion administrateur ---------------------------------------------
Etape 'Connexion administrateur'
$admin = Connexion -Email 'admin@3s-sgi.local' -MotDePasse $MotDePasseAdmin
Write-Host '   admin@3s-sgi.local connecté'

# --- 2. Comptes ---------------------------------------------------------------
# Création réservée aux ADMIN : ces appels échouent en 403 avec un simple manager.
Etape 'Création des comptes'

$comptesExistants = Invoke-Api -Methode GET -Chemin '/api/collaborateurs' -Jeton $admin
function Trouver-Compte { param([string]$Email) return $comptesExistants | Where-Object { $_.email -eq $Email } | Select-Object -First 1 }

$manager = Trouver-Compte $EmailManager
if (-not $manager) {
    $manager = Invoke-Api -Methode POST -Chemin '/api/collaborateurs/manager' -Jeton $admin -Corps @{
        nom = 'Durand'; prenom = 'Claire'; email = $EmailManager; motDePasse = $MotDePasseDemo
    }
}
Write-Host "   manager : $($manager.email) (id $($manager.id))"

$employes = @()
foreach ($e in @(
    @{ nom = 'Benali';   prenom = 'Karim'; email = $EmailEmploye1 },
    @{ nom = 'Marchand'; prenom = 'Lucie'; email = $EmailEmploye2 }
)) {
    $compte = Trouver-Compte $e.email
    if (-not $compte) {
        $compte = Invoke-Api -Methode POST -Chemin '/api/collaborateurs/employe' -Jeton $admin -Corps @{
            nom = $e.nom; prenom = $e.prenom; email = $e.email; motDePasse = $MotDePasseDemo
            manager = @{ id = $manager.id }
        }
    }
    $employes += $compte
    Write-Host "   employé : $($compte.email) (id $($compte.id))"
}

# --- 3. Client et projets -----------------------------------------------------
Etape 'Client et projets'
$client = Invoke-Api -Methode POST -Chemin '/api/clients' -Jeton $admin -Corps @{ nomClient = 'Manufacture Girard' }
Write-Host "   client : $($client.nomClient)"

$projets = @()
foreach ($p in @(
    @{ nom = 'Refonte du site vitrine'; debut = "$Annee-01-15"; fin = "$Annee-09-30" },
    @{ nom = 'Portail fournisseurs';    debut = "$Annee-03-01"; fin = "$Annee-12-15" }
)) {
    $projet = Invoke-Api -Methode POST -Chemin '/api/projets' -Jeton $admin -Corps @{
        nom = $p.nom; dateDebut = $p.debut; dateFin = $p.fin; client = @{ id = $client.id }
    }
    $projets += $projet
    Write-Host "   projet : $($projet.nom) (id $($projet.id))"
}

# --- 4. Affectations ----------------------------------------------------------
Etape 'Affectations'
$affectations = @(
    @{ collaborateur = $employes[0].id; projet = $projets[0].id; taux = 60 },
    @{ collaborateur = $employes[0].id; projet = $projets[1].id; taux = 40 },
    @{ collaborateur = $employes[1].id; projet = $projets[0].id; taux = 80 }
)
foreach ($a in $affectations) {
    Invoke-Api -Methode POST -Chemin '/api/affectations' -Jeton $admin -Corps @{
        collaborateurId = $a.collaborateur; projetId = $a.projet
        tauxAffectation = $a.taux; dateAffectation = (Get-Date -Format 'yyyy-MM-dd')
    } | Out-Null
    Write-Host "   collaborateur $($a.collaborateur) -> projet $($a.projet) à $($a.taux)%"
}

# --- 5. Quotas d'absence ------------------------------------------------------
# Karim : 5 jours de congés payés, dont 3 seront consommés plus bas.
# Il lui restera 2 jours, ce qui permet de montrer un refus de quota en démo.
Etape 'Quotas de congés'
Invoke-Api -Methode POST -Chemin '/api/absences/quotas' -Jeton $admin -Corps @{
    employeId = $employes[0].id; typeAbsence = 'CONGE_PAYE'; annee = $Annee; joursAlloues = 5
} | Out-Null
Invoke-Api -Methode POST -Chemin '/api/absences/quotas' -Jeton $admin -Corps @{
    employeId = $employes[1].id; typeAbsence = 'CONGE_PAYE'; annee = $Annee; joursAlloues = 20
} | Out-Null
Write-Host "   Karim : 5 jours alloués / Lucie : 20 jours alloués"

# --- 6. Imputations en attente de validation ----------------------------------
Etape 'Imputations (déposées par les employés eux-mêmes)'
$jetonKarim = Connexion -Email $EmailEmploye1 -MotDePasse $MotDePasseDemo
$jetonLucie = Connexion -Email $EmailEmploye2 -MotDePasse $MotDePasseDemo

$imputations = @(
    @{ jeton = $jetonKarim; employe = $employes[0].id; projet = $projets[0].id; heures = 7; nom = 'Maquettes page d''accueil'; jours = -3 },
    @{ jeton = $jetonKarim; employe = $employes[0].id; projet = $projets[1].id; heures = 5; nom = 'Cadrage besoins fournisseurs'; jours = -2 },
    @{ jeton = $jetonLucie; employe = $employes[1].id; projet = $projets[0].id; heures = 8; nom = 'Intégration du gabarit'; jours = -1 }
)
foreach ($i in $imputations) {
    Invoke-Api -Methode POST -Chemin "/api/imputations/employe/$($i.employe)" -Jeton $i.jeton -Corps @{
        projetId = $i.projet; dateImputation = (Get-Date).AddDays($i.jours).ToString('yyyy-MM-dd')
        heures = $i.heures; nom = $i.nom
    } | Out-Null
    Write-Host "   $($i.nom) — $($i.heures) h (en attente)"
}

# --- 7. Absence validée, qui consomme une partie du quota ---------------------
# L'application refuse toute absence dans le passé : on prend donc une fenêtre à
# venir. Lundi -> mercredi pour tomber sur exactement 3 jours ouvrés, quel que
# soit le jour où le script est joué.
Etape 'Absence de Karim, validée par sa manager'
$lundiProchain = (Get-Date).Date.AddDays(7)
while ($lundiProchain.DayOfWeek -ne [System.DayOfWeek]::Monday) { $lundiProchain = $lundiProchain.AddDays(1) }

$absence = Invoke-Api -Methode POST -Chemin "/api/absences/employe/$($employes[0].id)" -Jeton $jetonKarim -Corps @{
    typeAbsence = 'CONGE_PAYE'
    dateDebut = $lundiProchain.ToString('yyyy-MM-dd')
    dateFin = $lundiProchain.AddDays(2).ToString('yyyy-MM-dd')
    commentaireEmploye = 'Pont de printemps'
}
$jetonManager = Connexion -Email $EmailManager -MotDePasse $MotDePasseDemo
Invoke-Api -Methode POST -Chemin "/api/absences/$($absence.id)/valider" -Jeton $jetonManager | Out-Null
Write-Host "   absence de $($absence.nombreJours) jours validée"

# --- 8. Absence en attente, à valider pendant la démo -------------------------
Etape 'Absence de Lucie laissée en attente de validation'
$lundiSuivant = $lundiProchain.AddDays(14)
$enAttente = Invoke-Api -Methode POST -Chemin "/api/absences/employe/$($employes[1].id)" -Jeton $jetonLucie -Corps @{
    typeAbsence = 'CONGE_PAYE'
    dateDebut = $lundiSuivant.ToString('yyyy-MM-dd')
    dateFin = $lundiSuivant.AddDays(3).ToString('yyyy-MM-dd')
    commentaireEmploye = 'Congés familiaux'
}
Write-Host "   absence $($enAttente.id) — statut $($enAttente.statut)"

# --- 9. Contrôle du quota restant --------------------------------------------
Etape 'Quota restant de Karim'
$quotas = Invoke-Api -Methode GET -Chemin "/api/absences/quotas/employe/$($employes[0].id)/annee/$Annee" -Jeton $admin
foreach ($q in $quotas) {
    Write-Host "   $($q.typeAbsence) : $($q.joursPris) pris sur $($q.joursAlloues) alloués"
}

Write-Host "`nJeu de démonstration en place." -ForegroundColor Green
Write-Host "Comptes (mot de passe : $MotDePasseDemo)"
Write-Host "   manager  : $EmailManager"
Write-Host "   employés : $EmailEmploye1 / $EmailEmploye2"
Write-Host "   admin    : admin@3s-sgi.local (mot de passe fourni au lancement)"
