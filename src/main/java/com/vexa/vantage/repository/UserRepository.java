package com.vexa.vantage.repository;

import com.vexa.vantage.model.User;
import com.vexa.vantage.model.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    // Método mágico: Spring crea el SQL automáticamente al leer el nombre
    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);

    // Buscar usuarios por rol
    // Buscar usuarios por rol (activos)
    List<User> findByRole(RoleType role);

    // Buscar usuarios activos por empresa
    List<User> findAllByCompanyIdAndIsActiveTrue(Long companyId);

    // Buscar usuarios activos por rol
    List<User> findByRoleAndIsActiveTrue(RoleType role);

    Optional<User> findByEmailAndIsActiveTrue(String email);

    // Buscar todos los usuarios por empresa (activos e inactivos)
    List<User> findAllByCompanyId(Long companyId);
}