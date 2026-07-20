package com.SSS.SGI.dto;

import jakarta.validation.constraints.NotBlank;

public record RejeterAbsenceRequest(@NotBlank String motif) {}
