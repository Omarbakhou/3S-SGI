package com.SSS.SGI.dto;

import jakarta.validation.constraints.NotBlank;

public record RejeterImputationRequest(@NotBlank String motif) {}
