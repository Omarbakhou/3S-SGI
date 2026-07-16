package com.SSS.SGI.repository;

import com.SSS.SGI.entity.BudgetProjet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BudgetProjetRepository extends JpaRepository<BudgetProjet, Long>
    {
        @Query("SELECT b FROM BudgetProjet b WHERE b.client.id = :clientId")
        List<BudgetProjet> findByClientId(Long clientId);
    }

