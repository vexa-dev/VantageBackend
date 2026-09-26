package com.vexa.vantage.identity.application;

/**
 * Puerto (arquitectura hexagonal) que la capa de aplicación usa para
 * verificar una contraseña en texto plano contra su hash almacenado, sin
 * conocer el algoritmo de hashing concreto (por ejemplo BCrypt), que se
 * implementa en la capa de infraestructura.
 */
public interface PasswordVerifierPort {

    /**
     * Indica si {@code rawPassword} corresponde al hash {@code passwordHash}.
     */
    boolean matches(String rawPassword, String passwordHash);
}
