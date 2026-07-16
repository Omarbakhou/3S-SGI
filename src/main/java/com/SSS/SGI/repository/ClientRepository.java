package com.SSS.SGI.repository;

import com.SSS.SGI.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    @Query("SELECT c FROM Client c WHERE c.nomClient = :nomClient")
    Optional<Client> findByNomClient(String nomClient);

}

