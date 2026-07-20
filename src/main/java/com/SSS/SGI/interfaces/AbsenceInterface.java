package com.SSS.SGI.interfaces;

import com.SSS.SGI.entity.Employe;
import com.SSS.SGI.entity.enums.StatutAbsence;
import com.SSS.SGI.entity.enums.TypeAbsence;

import java.time.LocalDate;

/**
 * Contrat de lecture pour une absence, sur le même principe qu'ImputationInterface.
 */
public interface AbsenceInterface {
    Long getId();
    TypeAbsence getTypeAbsence();
    LocalDate getDateDebut();
    LocalDate getDateFin();
    Double getNombreJours();
    StatutAbsence getStatut();
    Employe getEmploye();
}
