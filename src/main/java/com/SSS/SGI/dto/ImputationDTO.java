package com.SSS.SGI.dto;

import com.SSS.SGI.entity.StatutImputation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImputationDTO {

    private Long id;
    private String nom;
    private String nomProjet;
    private Long projetId;
    private String nomEmploye;
    private Long employeId;
    private StatutImputation statut;
    private String managerValidateur;
    private LocalDateTime dateValidation;
}

