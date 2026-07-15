package com.SSS.SGI.repository;

import com.SSS.SGI.entity.Affectation;
import com.SSS.SGI.entity.AffectationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AffectationRepository extends JpaRepository<Affectation, AffectationId> {

    @Query("SELECT a FROM Affectation a WHERE a.collaborateur.id = :collaborateurId")
    List<Affectation> findByCollaborateurId(@Param("collaborateurId") Long collaborateurId);

    @Query("SELECT a FROM Affectation a WHERE a.projet.id = :projetId")
    List<Affectation> findByProjetId(@Param("projetId") Long projetId);

    @Query("SELECT a FROM Affectation a WHERE a.collaborateur.id = :collaborateurId AND a.projet.id = :projetId")
    Optional<Affectation> findByCollaborateurIdAndProjetId(@Param("collaborateurId") Long collaborateurId, @Param("projetId") Long projetId);

}

