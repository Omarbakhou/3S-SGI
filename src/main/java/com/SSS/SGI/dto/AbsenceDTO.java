package com.SSS.SGI.dto;

import com.SSS.SGI.entity.enums.StatutAbsence;
import com.SSS.SGI.entity.enums.TypeAbsence;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AbsenceDTO(
        Long id,
        TypeAbsence typeAbsence,
        LocalDate dateDebut,
        LocalDate dateFin,
        Double nombreJours,
        StatutAbsence statut,
        Long employeId,
        String employeNomComplet,
        /** "EMPLOYE" ou "MANAGER" : type réel du titulaire (employeId le désigne dans les deux cas). */
        String collaborateurType,
        Long managerValidateurId,
        LocalDateTime dateDemande,
        LocalDateTime dateValidation,
        String commentaireEmploye,
        String motifRejet,
        String justificatifUrl,
        boolean justificatifRequis
) {}
