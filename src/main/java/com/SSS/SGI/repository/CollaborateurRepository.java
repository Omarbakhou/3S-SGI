package com.SSS.SGI.repository;

import com.SSS.SGI.entity.Collaborateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CollaborateurRepository extends JpaRepository<Collaborateur, Long> {

    Optional<Collaborateur> findByEmail(String email);

    Optional<Collaborateur> findByNomAndPrenom(String nom, String prenom);

}

