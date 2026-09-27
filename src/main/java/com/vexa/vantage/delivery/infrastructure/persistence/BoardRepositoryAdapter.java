package com.vexa.vantage.delivery.infrastructure.persistence;

import com.vexa.vantage.delivery.application.BoardRepositoryPort;
import com.vexa.vantage.delivery.domain.Board;
import com.vexa.vantage.shared.domain.ProjectId;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adapter (arquitectura hexagonal) que implementa {@link BoardRepositoryPort}
 * sobre JPA/Spring Data.
 */
@Component
public class BoardRepositoryAdapter implements BoardRepositoryPort {

    private final BoardJpaRepository repository;

    public BoardRepositoryAdapter(BoardJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Board> findByProjectId(ProjectId projectId) {
        return repository.findByProjectId(Long.parseLong(projectId.value())).map(BoardMapper::toDomain);
    }
}
