package com.SSS.SGI.dto;

import com.SSS.SGI.entity.enums.TypeAbsence;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateAbsenceRequest(
        @NotNull TypeAbsence typeAbsence,
        @NotNull LocalDate dateDebut,
        @NotNull LocalDate dateFin,
        String commentaireEmploye
) {}
