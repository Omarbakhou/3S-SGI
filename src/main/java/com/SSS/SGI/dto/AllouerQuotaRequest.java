package com.SSS.SGI.dto;

import com.SSS.SGI.entity.enums.TypeAbsence;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AllouerQuotaRequest(
        @NotNull Long employeId,
        @NotNull TypeAbsence typeAbsence,
        @NotNull Integer annee,
        @NotNull @Positive Double joursAlloues
) {}
