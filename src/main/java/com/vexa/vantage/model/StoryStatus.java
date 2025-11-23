package com.vexa.vantage.model;

public enum StoryStatus {
    BACKLOG, // En espera de ser priorizada
    TODO, // Lista para trabajar en este Sprint
    DOING, // En desarrollo
    TESTING, // En revisión (QA o PO)
    DONE // Terminada
}