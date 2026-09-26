package com.vexa.vantage.identity.infrastructure.persistence;

import com.vexa.vantage.identity.application.RefreshTokenRepositoryPort;
import com.vexa.vantage.identity.domain.RefreshToken;
import com.vexa.vantage.shared.domain.UserId;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Adapter (arquitectura hexagonal) que implementa {@link RefreshTokenRepositoryPort}
 * sobre JPA/Spring Data. Su nombre termina en {@code RepositoryAdapter} y
 * vive en un paquete {@code infrastructure.persistence} a propósito, para
 * que {@code TenantFilterAspect} lo intercepte automáticamente (aunque,
 * como {@link RefreshTokenJpaEntity} no tiene columna {@code tenant_id}, el
 * filtro Hibernate habilitado por ese aspecto no tiene ningún efecto aquí).
 */
@Component
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepositoryPort {

    private final RefreshTokenJpaRepository repository;

    public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(RefreshToken refreshToken) {
        RefreshTokenJpaEntity existing = repository.findByJti(refreshToken.jti()).orElse(null);
        repository.save(RefreshTokenMapper.toEntity(refreshToken, existing));
    }

    @Override
    public Optional<RefreshToken> findByJti(String jti) {
        return repository.findByJti(jti).map(RefreshTokenMapper::toDomain);
    }

    @Override
    public void revokeAllForUser(UserId userId) {
        List<RefreshTokenJpaEntity> tokens = repository.findAllByUserId(Long.parseLong(userId.value()));
        Instant now = Instant.now();
        tokens.stream()
                .filter(token -> token.getRevokedAt() == null)
                .forEach(token -> token.setRevokedAt(now));
        repository.saveAll(tokens);
    }
}
