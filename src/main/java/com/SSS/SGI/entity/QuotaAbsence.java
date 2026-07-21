package com.SSS.SGI.entity;

import com.SSS.SGI.entity.enums.TypeAbsence;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "quota_absence",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_quota_employe_type_annee",
                columnNames = {"id_employe", "type_absence", "annee"}))
@Getter
@Setter
@NoArgsConstructor
public class QuotaAbsence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_quota")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_employe", nullable = false)
    private Employe employe;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_absence", nullable = false, length = 40)
    private TypeAbsence typeAbsence;

    @Column(name = "annee", nullable = false)
    private Integer annee;

    @Column(name = "jours_alloues", nullable = false)
    private Double joursAlloues;

    @Column(name = "jours_pris", nullable = false)
    private Double joursPris = 0.0;

    @Version
    @Column(name = "version")
    private Long version;

    /** Non persisté : dérivé de joursAlloues - joursPris. */
    @Transient
    public Double getJoursRestants() {
        return joursAlloues - joursPris;
    }
}
