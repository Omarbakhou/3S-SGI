package com.SSS.SGI.repository;

import com.SSS.SGI.entity.Projet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjetRepository extends JpaRepository<Projet, Long> {

    Optional<Projet> findByNom(String nom);

    List<Projet> findByClientId(Long clientId);

}

