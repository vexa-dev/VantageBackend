package com.vexa.vantage.repository;

import com.vexa.vantage.model.Project;
import com.vexa.vantage.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    // Encuentra proyectos donde el usuario es el Dueño
    List<Project> findByOwner(User owner);

    // Encuentra proyectos donde el usuario es Miembro (Equipo)
    List<Project> findByMembersContaining(User user);

    List<Project> findByScrumMaster(User scrumMaster);

    // Seguridad Multi-tenant: Encontrar todos los proyectos de una empresa
    List<Project> findByCompanyId(Long companyId);

    // Encontrar proyectos donde el usuario es miembro Y pertenece a su empresa
    List<Project> findByMembersContainingAndCompanyId(User user, Long companyId);
}