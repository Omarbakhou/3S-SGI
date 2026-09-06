package com.SSS.SGI.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateImputationRequest(
        @NotNull Long projetId,
        @NotNull LocalDate dateImputation,
        @NotNull Double heures,
        @NotBlank String nom
) {}
