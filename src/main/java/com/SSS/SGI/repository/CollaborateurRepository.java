package com.SSS.SGI.repository;

import com.SSS.SGI.entity.Collaborateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CollaborateurRepository extends JpaRepository<Collaborateur, Long> {
    @Query("SELECT c FROM Collaborateur c WHERE c.email = :email")
    Optional<Collaborateur> findByEmail(String email);

    @Query("SELECT c FROM Collaborateur c WHERE c.nom = :nom AND c.prenom = :prenom")
    Optional<Collaborateur> findByNomAndPrenom(String nom, String prenom);

}

