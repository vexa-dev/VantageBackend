package com.vexa.vantage.identity.application;

import com.vexa.vantage.identity.domain.AppUser;
import com.vexa.vantage.shared.domain.UserId;

import java.util.Optional;

/**
 * Puerto (arquitectura hexagonal) que la capa de aplicación usa para
 * consultar y persistir {@link AppUser}, sin conocer el mecanismo de
 * persistencia concreto (JPA, memoria, etc.), que se implementa en la capa
 * de infraestructura.
 */
public interface AppUserRepositoryPort {

    /**
     * Busca un usuario por su email.
     */
    Optional<AppUser> findByEmail(String email);

    /**
     * Busca un usuario por su identificador.
     */
    Optional<AppUser> findById(UserId id);

    /**
     * Guarda (inserta o actualiza) un usuario.
     */
    void save(AppUser user);
}
