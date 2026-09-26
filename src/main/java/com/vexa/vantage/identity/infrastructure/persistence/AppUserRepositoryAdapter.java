package com.vexa.vantage.identity.infrastructure.persistence;

import com.vexa.vantage.identity.application.AppUserRepositoryPort;
import com.vexa.vantage.identity.domain.AppUser;
import com.vexa.vantage.shared.domain.UserId;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adapter (arquitectura hexagonal) que implementa {@link AppUserRepositoryPort}
 * sobre JPA/Spring Data. Su nombre termina en {@code RepositoryAdapter} y
 * vive en un paquete {@code infrastructure.persistence} a propósito, para
 * que {@code TenantFilterAspect} lo intercepte automáticamente y acote las
 * lecturas al tenant vigente en {@code TenantContext} (cuando hay uno
 * establecido; {@code findByEmail} durante el login se ejecuta típicamente
 * sin tenant en contexto, ya que aún no se conoce, por lo que ve todos los
 * tenants).
 */
@Component
public class AppUserRepositoryAdapter implements AppUserRepositoryPort {

    private final AppUserSpringDataRepository repository;

    public AppUserRepositoryAdapter(AppUserSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<AppUser> findByEmail(String email) {
        return repository.findByEmail(email).map(AppUserMapper::toDomain);
    }

    @Override
    public Optional<AppUser> findById(UserId id) {
        return repository.findById(Long.parseLong(id.value())).map(AppUserMapper::toDomain);
    }

    @Override
    public void save(AppUser user) {
        repository.save(AppUserMapper.toEntity(user));
    }
}
