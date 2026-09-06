package com.SSS.SGI.repository;

import com.SSS.SGI.entity.QuotaAbsence;
import com.SSS.SGI.entity.enums.TypeAbsence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuotaAbsenceRepository extends JpaRepository<QuotaAbsence, Long> {

    Optional<QuotaAbsence> findByCollaborateur_IdAndTypeAbsenceAndAnnee(
            Long collaborateurId, TypeAbsence typeAbsence, Integer annee);

    List<QuotaAbsence> findByCollaborateur_IdAndAnnee(Long collaborateurId, Integer annee);
}
