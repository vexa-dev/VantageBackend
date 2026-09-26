package com.vexa.vantage.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de Spring Data para {@link RefreshTokenJpaEntity}, colaborador
 * interno de {@link RefreshTokenRepositoryAdapter}.
 */
public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, Long> {

    Optional<RefreshTokenJpaEntity> findByJti(String jti);

    List<RefreshTokenJpaEntity> findAllByUserId(Long userId);
}
