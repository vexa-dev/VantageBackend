package com.vexa.vantage.delivery.domain;

import com.vexa.vantage.shared.domain.DomainValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica el cálculo WSJF (Weighted Shortest Job First) a partir de
 * {@code businessValue}/{@code urgency}/{@code storyPoints}, y confirma que
 * {@link WsjfScore} es un valor puramente computado: nunca se persiste
 * (sin columna dedicada, sin anotaciones JPA — verificado además por
 * {@code ArchitectureRulesTest}, que prohíbe cualquier dependencia de JPA en
 * este paquete de dominio).
 */
class WsjfScoreTest {

    @Test
    void computesScoreFromBusinessValueUrgencyAndStoryPoints() {
        WsjfScore score = WsjfScore.compute(80, 60, 5);

        assertThat(score.value()).isEqualTo(28.0);
    }

    @Test
    void triangulatesWithDifferentInputsToForceRealDivision() {
        WsjfScore score = WsjfScore.compute(50, 30, 8);

        assertThat(score.value()).isEqualTo(10.0);
    }

    @Test
    void fallsBackToDefaultEffectivePointsWhenStoryPointsIsNull() {
        WsjfScore score = WsjfScore.compute(40, 20, null);

        assertThat(score.value()).isEqualTo(60.0);
    }

    @Test
    void fallsBackToDefaultEffectivePointsWhenStoryPointsIsZeroOrNegative() {
        WsjfScore score = WsjfScore.compute(40, 20, 0);

        assertThat(score.value()).isEqualTo(60.0);
    }

    @Test
    void treatsNullBusinessValueAndUrgencyAsZero() {
        WsjfScore score = WsjfScore.compute(null, null, 4);

        assertThat(score.value()).isEqualTo(0.0);
    }

    @Test
    void rejectsBusinessValueAboveMaximum() {
        assertThatThrownBy(() -> WsjfScore.compute(101, 10, 5))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("businessValue");
    }

    @Test
    void rejectsNegativeUrgency() {
        assertThatThrownBy(() -> WsjfScore.compute(10, -1, 5))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("urgency");
    }
}
