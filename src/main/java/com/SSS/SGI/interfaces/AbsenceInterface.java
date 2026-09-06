package com.SSS.SGI.interfaces;

import com.SSS.SGI.entity.Collaborateur;
import com.SSS.SGI.entity.enums.StatutAbsence;
import com.SSS.SGI.entity.enums.TypeAbsence;

import java.time.LocalDate;

/**
 * Contrat de lecture pour une absence, sur le même principe qu'ImputationInterface.
 * Le titulaire est un Collaborateur (Employe ou Manager) : les managers peuvent
 * déposer des demandes d'absence au même titre que les employés.
 */
public interface AbsenceInterface {
    Long getId();
    TypeAbsence getTypeAbsence();
    LocalDate getDateDebut();
    LocalDate getDateFin();
    Double getNombreJours();
    StatutAbsence getStatut();
    Collaborateur getCollaborateur();
}
