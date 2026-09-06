package com.SSS.SGI.repository;

import com.SSS.SGI.entity.Absence;
import com.SSS.SGI.entity.enums.StatutAbsence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AbsenceRepository extends JpaRepository<Absence, Long> {

    List<Absence> findByCollaborateur_Id(Long collaborateurId);

    List<Absence> findByStatut(StatutAbsence statut);

    /**
     * Absences du même collaborateur (employé ou manager) qui chevauchent la période
     * donnée, en ignorant celles déjà rejetées ou annulées.
     */
    @Query("""
        SELECT a FROM Absence a
        WHERE a.collaborateur.id = :collaborateurId
        AND a.statut <> com.SSS.SGI.entity.enums.StatutAbsence.REJETEE
        AND a.statut <> com.SSS.SGI.entity.enums.StatutAbsence.ANNULEE
        AND a.dateDebut <= :dateFin AND a.dateFin >= :dateDebut
        """)
    List<Absence> findChevauchements(@Param("collaborateurId") Long collaborateurId,
                                      @Param("dateDebut") LocalDate dateDebut,
                                      @Param("dateFin") LocalDate dateFin);
}
