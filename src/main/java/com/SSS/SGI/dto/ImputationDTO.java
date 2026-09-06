package com.SSS.SGI.dto;

import com.SSS.SGI.entity.StatutImputation;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ImputationDTO(
        Long id,
        String nom,
        LocalDate dateImputation,
        Double heures,
        StatutImputation statut,
        Long projetId,
        String projetNom,
        Long employeId,
        String employeNomComplet,
        Long managerValidateurId,
        LocalDateTime dateValidation,
        String motifRejet
) {}
