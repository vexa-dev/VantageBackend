package com.vexa.vantage.identity.infrastructure;

import com.vexa.vantage.identity.application.PasswordVerifierPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Adapter (arquitectura hexagonal) que implementa {@link PasswordVerifierPort}
 * usando BCrypt. Construye su propio {@link PasswordEncoder} en lugar de
 * depender del bean {@code passwordEncoder()} del {@code WebSecurityConfig}
 * legado (que se eliminará en una fase futura), para que este adapter sea
 * autocontenido y no se rompa cuando esa clase desaparezca.
 */
@Component
public class BCryptPasswordVerifierAdapter implements PasswordVerifierPort {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return passwordEncoder.matches(rawPassword, passwordHash);
    }
}
