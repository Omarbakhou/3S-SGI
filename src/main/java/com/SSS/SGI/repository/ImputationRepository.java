package com.SSS.SGI.repository;

import com.SSS.SGI.entity.Imputation;
import com.SSS.SGI.entity.StatutImputation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImputationRepository extends JpaRepository<Imputation, Long> {
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

