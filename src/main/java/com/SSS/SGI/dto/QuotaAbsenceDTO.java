package com.SSS.SGI.dto;

import com.SSS.SGI.entity.enums.TypeAbsence;

public record QuotaAbsenceDTO(
        Long id,
        Long employeId,
        TypeAbsence typeAbsence,
        Integer annee,
        Double joursAlloues,
        Double joursPris,
        Double joursRestants
) {}
