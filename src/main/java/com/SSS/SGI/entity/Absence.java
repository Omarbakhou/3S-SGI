package com.SSS.SGI.entity;

import com.SSS.SGI.entity.enums.StatutAbsence;
import com.SSS.SGI.entity.enums.TypeAbsence;
import com.SSS.SGI.exception.JustificatifManquantException;
import com.SSS.SGI.interfaces.AbsenceInterface;
import com.SSS.SGI.interfaces.ValidationAbsenceInterface;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "absence")
@Getter
@Setter
@NoArgsConstructor
public class Absence implements AbsenceInterface, ValidationAbsenceInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_absence")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_absence", nullable = false, length = 40)
    private TypeAbsence typeAbsence;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(name = "nombre_jours", nullable = false)
    private Double nombreJours;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutAbsence statut = StatutAbsence.EN_ATTENTE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_employe", nullable = false)
    private Employe employe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_manager_validateur")
    private Manager managerValidateur;

    @Column(name = "date_demande", nullable = false)
    private LocalDateTime dateDemande = LocalDateTime.now();

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @Column(name = "commentaire_employe", length = 500)
    private String commentaireEmploye;

    @Column(name = "motif_rejet", length = 500)
    private String motifRejet;

    @Column(name = "justificatif_url", length = 255)
    private String justificatifUrl;

    @Column(name = "date_envoi_justificatif")
    private LocalDate dateEnvoiJustificatif;

    // ---- ValidationAbsenceInterface ----

    @Override
    public void valider(Manager manager) {
        if (!peutEtreValidee()) {
            throw new IllegalStateException("Seule une absence EN_ATTENTE peut être validée.");
        }
        if (typeAbsence.isJustificatifObligatoire()
                && (justificatifUrl == null || justificatifUrl.isBlank())) {
            throw new JustificatifManquantException(
                    "Un justificatif est obligatoire pour le type " + typeAbsence + " avant validation.");
        }
        this.statut = StatutAbsence.VALIDEE;
        this.managerValidateur = manager;
        this.dateValidation = LocalDateTime.now();
    }

    @Override
    public void rejeter(Manager manager, String motif) {
        if (!peutEtreValidee()) {
            throw new IllegalStateException("Seule une absence EN_ATTENTE peut être rejetée.");
        }
        this.statut = StatutAbsence.REJETEE;
        this.managerValidateur = manager;
        this.dateValidation = LocalDateTime.now();
        this.motifRejet = motif;
    }

    @Override
    public boolean peutEtreValidee() {
        return this.statut == StatutAbsence.EN_ATTENTE;
    }
}
