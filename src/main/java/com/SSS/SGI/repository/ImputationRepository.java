package com.SSS.SGI.repository;

import com.SSS.SGI.entity.Imputation;
import com.SSS.SGI.entity.StatutImputation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImputationRepository extends JpaRepository<Imputation, Long> {

    List<Imputation> findByEmployeId(Long employeId);

    List<Imputation> findByProjetId(Long projetId);

    List<Imputation> findByStatut(StatutImputation statut);

    List<Imputation> findByManagerValidateurId(Long managerId);

    List<Imputation> findByEmployeIdAndStatut(Long employeId, StatutImputation statut);

}

