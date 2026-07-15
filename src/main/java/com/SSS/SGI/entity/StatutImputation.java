package com.SSS.SGI.entity;

public enum StatutImputation {
    EN_ATTENTE("En attente"),
    VALIDEE("Validée"),
    REJETEE("Rejetée");

    private final String label;

    StatutImputation(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

