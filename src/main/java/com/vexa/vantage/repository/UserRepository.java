package com.vexa.vantage.repository;

import com.vexa.vantage.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // Método mágico: Spring crea el SQL automáticamente al leer el nombre
    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);
}