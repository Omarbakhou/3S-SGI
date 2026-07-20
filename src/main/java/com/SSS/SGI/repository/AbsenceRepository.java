package com.SSS.SGI.repository;

import com.SSS.SGI.entity.Absence;
import com.SSS.SGI.entity.enums.StatutAbsence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AbsenceRepository extends JpaRepository<Absence, Long> {

    List<Absence> findByEmploye_Id(Long employeId);

    List<Absence> findByStatut(StatutAbsence statut);

    /**
     * Absences du même employé qui chevauchent la période donnée,
     * en ignorant celles déjà rejetées ou annulées.
     */
    @Query("""
        SELECT a FROM Absence a
        WHERE a.employe.id = :employeId
        AND a.statut <> com.SSS.SGI.entity.enums.StatutAbsence.REJETEE
        AND a.statut <> com.SSS.SGI.entity.enums.StatutAbsence.ANNULEE
        AND a.dateDebut <= :dateFin AND a.dateFin >= :dateDebut
        """)
    List<Absence> findChevauchements(@Param("employeId") Long employeId,
                                      @Param("dateDebut") LocalDate dateDebut,
                                      @Param("dateFin") LocalDate dateFin);
}
