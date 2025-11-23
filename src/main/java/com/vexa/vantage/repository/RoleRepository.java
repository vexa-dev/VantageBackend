package com.vexa.vantage.repository;

import com.vexa.vantage.model.Role;
import com.vexa.vantage.model.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(RoleType name);
}