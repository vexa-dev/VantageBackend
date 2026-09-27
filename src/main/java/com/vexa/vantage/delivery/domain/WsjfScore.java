package com.vexa.vantage.delivery.domain;

import com.vexa.vantage.shared.domain.DomainValidationException;

/**
 * Objeto de valor que representa el puntaje WSJF (Weighted Shortest Job
 * First) de un {@link WorkItem}: {@code (businessValue + urgency) /
 * effectivePoints}.
 *
 * <p>Es un valor puramente computado bajo demanda a partir de
 * {@code businessValue}/{@code urgency}/{@code storyPoints} — nunca se
 * persiste (no existe una columna {@code priority_score} en el esquema; el
 * antiguo cálculo vía {@code @PrePersist}/{@code @PreUpdate} queda eliminado
 * a propósito). El respaldo histórico de "talla de camiseta" (t-shirt size)
 * cuando no hay {@code storyPoints} es ahora una preocupación exclusiva de
 * la migración de datos (ETL), que resuelve la talla a puntos concretos
 * antes de que el valor llegue a este objeto de dominio.
 *
 * <p>Java puro: sin dependencia de Spring, JPA ni Jackson (verificado por
 * {@code ArchitectureRulesTest}).
 */
public record WsjfScore(double value) {

    private static final double DEFAULT_EFFECTIVE_POINTS = 1.0;
    private static final int MIN_SCALE = 0;
    private static final int MAX_SCALE = 100;

    public WsjfScore {
        if (value < 0) {
            throw new DomainValidationException("WsjfScore.value must not be negative");
        }
    }

    /**
     * Calcula el puntaje WSJF a partir de los tres insumos crudos del work
     * item. {@code businessValue}/{@code urgency} nulos se tratan como cero,
     * igual que el cálculo histórico; {@code storyPoints} nulo o menor o
     * igual a cero recae en {@link #DEFAULT_EFFECTIVE_POINTS}.
     *
     * @throws DomainValidationException si {@code businessValue}/{@code urgency}
     *                                    quedan fuera del rango 0-100 (el
     *                                    mismo rango exigido por los
     *                                    {@code CHECK} de {@code work_item})
     */
    public static WsjfScore compute(Integer businessValue, Integer urgency, Integer storyPoints) {
        int resolvedBusinessValue = requireInRange(businessValue, "businessValue");
        int resolvedUrgency = requireInRange(urgency, "urgency");
        double effectivePoints = resolveEffectivePoints(storyPoints);
        return new WsjfScore((resolvedBusinessValue + resolvedUrgency) / effectivePoints);
    }

    private static double resolveEffectivePoints(Integer storyPoints) {
        if (storyPoints == null || storyPoints <= 0) {
            return DEFAULT_EFFECTIVE_POINTS;
        }
        return storyPoints;
    }

    private static int requireInRange(Integer rawValue, String fieldName) {
        int resolved = rawValue != null ? rawValue : 0;
        if (resolved < MIN_SCALE || resolved > MAX_SCALE) {
            throw new DomainValidationException(
                    "WsjfScore." + fieldName + " must be between " + MIN_SCALE + " and " + MAX_SCALE);
        }
        return resolved;
    }
}
