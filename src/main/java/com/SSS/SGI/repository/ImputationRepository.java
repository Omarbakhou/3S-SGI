package com.SSS.SGI.repository;

import com.SSS.SGI.entity.Imputation;
import com.SSS.SGI.entity.StatutImputation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ImputationRepository extends JpaRepository<Imputation, Long> {

    @Query("SELECT i FROM Imputation i WHERE i.employe.id = :employeId AND i.projet.id = :projetId "
            + "AND i.dateImputation = :date AND (:excludeId IS NULL OR i.id <> :excludeId)")
    Optional<Imputation> findDoublon(Long employeId, Long projetId, LocalDate date, Long excludeId);

    @Query("SELECT COALESCE(SUM(i.heures), 0) FROM Imputation i "
            + "WHERE i.projet.id = :projetId AND i.statut = 'VALIDEE'")
    Double sumHeuresValideesByProjet(Long projetId);

    @Query("SELECT COALESCE(SUM(i.heures), 0) FROM Imputation i WHERE i.employe.id = :employeId "
            + "AND i.dateImputation = :date AND i.statut <> 'REJETEE' AND (:excludeId IS NULL OR i.id <> :excludeId)")
    Double sumHeuresDuJour(Long employeId, LocalDate date, Long excludeId);
    @Query("SELECT i FROM Imputation i WHERE i.employe.id = :employeId AND i.projet.id = :projetId")
    List<Imputation> findByEmployeIdAndProjetId(Long employeId, Long projetId);

    @Query("SELECT i FROM Imputation i WHERE i.employe.id = :employeId")
    List<Imputation> findByEmployeId(Long employeId);

    @Query("SELECT i FROM Imputation i WHERE i.projet.id = :projetId")
    List<Imputation> findByProjetId(Long projetId);

    @Query("SELECT i FROM Imputation i WHERE i.statut = :statut")
    List<Imputation> findByStatut(StatutImputation statut);

    @Query("SELECT i FROM Imputation i WHERE i.managerValidateur.id = :managerId")
    List<Imputation> findByManagerValidateurId(Long managerId);

    @Query("SELECT i FROM Imputation i WHERE i.employe.id = :employeId AND i.statut = :statut")
    List<Imputation> findByEmployeIdAndStatut(Long employeId, StatutImputation statut);

}

