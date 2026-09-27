package com.vexa.vantage.delivery.infrastructure.persistence;

import com.vexa.vantage.delivery.domain.Board;
import com.vexa.vantage.delivery.domain.BoardId;
import com.vexa.vantage.shared.domain.ProjectId;

/**
 * Mapper manual entre {@link BoardJpaEntity} (infraestructura) y
 * {@link Board} (dominio puro).
 */
public final class BoardMapper {

    private BoardMapper() {
    }

    public static Board toDomain(BoardJpaEntity entity) {
        return new Board(
                BoardId.of(String.valueOf(entity.getId())),
                ProjectId.of(String.valueOf(entity.getProjectId())),
                entity.getName());
    }

    public static BoardJpaEntity toEntity(Board domain) {
        BoardJpaEntity entity = new BoardJpaEntity();
        if (domain.id() != null) {
            entity.setId(Long.parseLong(domain.id().value()));
        }
        entity.setProjectId(Long.parseLong(domain.projectId().value()));
        entity.setName(domain.name());
        return entity;
    }
}
