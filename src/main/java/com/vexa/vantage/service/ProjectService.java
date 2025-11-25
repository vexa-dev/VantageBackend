package com.vexa.vantage.service;

import com.vexa.vantage.model.Project;
import com.vexa.vantage.model.User;
import com.vexa.vantage.repository.ProjectRepository;
import com.vexa.vantage.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    // Crear un nuevo proyecto
    public Project createProject(String name, String description, String ownerEmail, String icon, Long scrumMasterId) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Project project = new Project(name, description, owner);
        project.setIcon(icon);

        if (scrumMasterId != null) {
            User scrumMaster = userRepository.findById(scrumMasterId)
                    .orElseThrow(() -> new RuntimeException("Scrum Master no encontrado"));
            project.setScrumMaster(scrumMaster);
            project.getMembers().add(scrumMaster);
        }

        // El dueño es automáticamente miembro del equipo
        project.getMembers().add(owner);

        return projectRepository.save(project);
    }

    // Obtener proyectos donde soy miembro
    public List<Project> getMyProjects(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return projectRepository.findByMembersContaining(user);
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
}