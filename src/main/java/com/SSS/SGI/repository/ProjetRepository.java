package com.SSS.SGI.repository;

import com.SSS.SGI.entity.Projet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjetRepository extends JpaRepository<Projet, Long> {
    @Query("SELECT p FROM Projet p WHERE p.nom = :nom")
    Optional<Projet> findProjetByNom(String nom);

    @Query("SELECT p FROM Projet p WHERE p.client.id = :clientId")
    List<Projet> findProjetByClientId(Long clientId);

}

