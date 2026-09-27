package com.vexa.vantage.delivery.application;

import com.vexa.vantage.delivery.domain.Board;
import com.vexa.vantage.shared.domain.ProjectId;

import java.util.Optional;

/**
 * Puerto (arquitectura hexagonal) que la capa de aplicación usa para
 * consultar y persistir {@link Board}, sin conocer el mecanismo de
 * persistencia concreto (JPA, memoria, etc.), implementado en la capa de
 * infraestructura.
 *
 * <p>{@code board} es una vista guardada delgada (solo nombre + proyecto);
 * un proyecto es completamente usable con cero filas de {@code board}, por
 * lo que {@link #findByProjectId} devuelve vacío en ese caso — no es un error.
 */
public interface BoardRepositoryPort {

    /**
     * Busca el tablero guardado del proyecto indicado, si existe.
     */
    Optional<Board> findByProjectId(ProjectId projectId);
}
