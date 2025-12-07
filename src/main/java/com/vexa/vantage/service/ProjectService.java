package com.vexa.vantage.service;

import com.vexa.vantage.model.Project;
import com.vexa.vantage.model.User;
import com.vexa.vantage.repository.ProjectRepository;
import com.vexa.vantage.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    // Crear un nuevo proyecto
    public Project createProject(String name, String description, String icon, Long poId, Long smId, List<Long> devIds,
            LocalDate startDate, LocalDate endDate) {

        User owner = userRepository.findById(poId)
                .orElseThrow(() -> new RuntimeException("Product Owner no encontrado"));

        Project project = new Project(name, description, owner);
        project.setIcon(icon);
        project.setStartDate(startDate);
        project.setEndDate(endDate);

        if (smId != null) {
            User scrumMaster = userRepository.findById(smId)
                    .orElseThrow(() -> new RuntimeException("Scrum Master no encontrado"));
            project.setScrumMaster(scrumMaster);
            project.getMembers().add(scrumMaster);
        }

        if (devIds != null && !devIds.isEmpty()) {
            List<User> developers = userRepository.findAllById(devIds);
            project.getMembers().addAll(developers);
        }

        // El dueño es automáticamente miembro del equipo
        project.getMembers().add(owner);

        // Asignar proyecto a la empresa del PO
        if (owner.getCompany() != null) {
            project.setCompany(owner.getCompany());
        }

        return projectRepository.save(project);
    }

    // Obtener proyectos (Mis proyectos o TODOS si soy ADMIN/OWNER, asegurando
    // legacy)
    public List<Project> getMyProjects(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        java.util.Set<Project> projects = new java.util.HashSet<>();

        // 1. Projects where user is explicitly MEMBERS or OWNER (Basic Visibility)
        projects.addAll(projectRepository.findByMembersContaining(user));
        projects.addAll(projectRepository.findByOwner(user));

        // 2. Company Global View
        if (user.getCompany() != null) {
            projects.addAll(projectRepository.findByCompanyId(user.getCompany().getId()));
        }

        // 3. ADMIN/OWNER Fallback for Legacy/Orphan Projects
        // If I am Admin/Owner, I should see projects that have NO company assigned yet
        // (Legacy Support)
        // 3. Fallback for Legacy/Orphan Projects (Global Visibility for unassigned)
        // Ensure projects with NO company are visible to everyone (Legacy Support)
        // UPDATE: Only show orphans to users who are ALSO orphan (no company).
        // Company users should not see unrelated orphans to prevent data leaks.
        if (user.getCompany() == null) {
            List<Project> allProjects = projectRepository.findAll();
            for (Project p : allProjects) {
                if (p.getCompany() == null) {
                    projects.add(p);
                }
            }
        }

        // 4. Strict Isolation Filter: Remove projects that belong to a DIFFERENT
        // company
        if (user.getCompany() != null) {
            projects.removeIf(p -> p.getCompany() != null
                    && !p.getCompany().getId().equals(user.getCompany().getId()));
        }

        return new java.util.ArrayList<>(projects);
    }

    // Obtener TODOS los proyectos (para el directorio)
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    // Buscar por ID
    public Project findById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));
    }

    public Project updateMembers(Long projectId, List<Long> userIds) {
        Project project = findById(projectId);

        // Mantener PO y SM
        User owner = project.getOwner();
        User scrumMaster = project.getScrumMaster();

        // Limpiar miembros actuales
        project.getMembers().clear();

        // Re-agregar PO y SM
        if (owner != null)
            project.getMembers().add(owner);
        if (scrumMaster != null)
            project.getMembers().add(scrumMaster);

        // Agregar los nuevos desarrolladores seleccionados
        List<User> users = userRepository.findAllById(userIds);
        project.getMembers().addAll(users);

        return projectRepository.save(project);
    }

    public Project updateProject(Long id, String name, String description, String icon, LocalDate startDate,
            LocalDate endDate, String status, Long poId, Long smId, List<Long> devIds) {
        Project project = findById(id);
        project.setName(name);
        project.setDescription(description);
        project.setIcon(icon);
        project.setStartDate(startDate);
        project.setEndDate(endDate);
        project.setStatus(status);

        if (poId != null) {
            User newOwner = userRepository.findById(poId)
                    .orElseThrow(() -> new RuntimeException("Product Owner no encontrado"));
            project.setOwner(newOwner);
        }

        if (smId != null) {
            User newSM = userRepository.findById(smId)
                    .orElseThrow(() -> new RuntimeException("Scrum Master no encontrado"));
            project.setScrumMaster(newSM);
        }

        // Reconstruir lista de miembros si se envían devIds
        if (devIds != null) {
            project.getMembers().clear();

            // Agregar siempre al Owner actual
            if (project.getOwner() != null) {
                project.getMembers().add(project.getOwner());
            }

            // Agregar siempre al SM actual
            if (project.getScrumMaster() != null) {
                project.getMembers().add(project.getScrumMaster());
            }

            // Agregar nuevos devs
            if (!devIds.isEmpty()) {
                List<User> developers = userRepository.findAllById(devIds);
                project.getMembers().addAll(developers);
            }
        }

        return projectRepository.save(project);
    }

    // Obtener proyectos especificos para el BACKLOG (Dropdown)
    // ADMIN/OWNER: Ven todo (Global Legacy)
    // PO/SM/DEV: Ven SOLO sus asignaciones (Strict Assigned)
    public List<Project> getProjectsForBacklog(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        java.util.Set<Project> projects = new java.util.HashSet<>();

        // Si es ADMIN u OFFICER (Owner), usa la logica global (ver todo lo de su
        // empresa +
        // legacy)
        if (user.getRole() == com.vexa.vantage.model.RoleType.ROLE_ADMIN
                || user.getRole() == com.vexa.vantage.model.RoleType.ROLE_OWNER) {
            return getMyProjects(userEmail);
        }

        // Si es un ROL OPERATIVO (PO, SM, DEV), solo ve asignaciones explicitas
        projects.addAll(projectRepository.findByMembersContaining(user));
        projects.addAll(projectRepository.findByOwner(user));

        // CRITICAL SECURITY: Filtrar siempre proyectos de otras empresas
        if (user.getCompany() != null) {
            projects.removeIf(p -> p.getCompany() != null
                    && !p.getCompany().getId().equals(user.getCompany().getId()));
        }

        return new java.util.ArrayList<>(projects);
    }
}