package com.vexa.vantage.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio de Spring Data para {@link AppUserJpaEntity}, colaborador
 * interno de {@link AppUserRepositoryAdapter}. Se nombra
 * {@code SpringDataRepository} (en lugar de {@code JpaRepository}, como los
 * demás agregados) para distinguirlo claramente del adapter de puerto que sí
 * termina en {@code RepositoryAdapter}.
 */
public interface AppUserSpringDataRepository extends JpaRepository<AppUserJpaEntity, Long> {

    Optional<AppUserJpaEntity> findByEmail(String email);
}
