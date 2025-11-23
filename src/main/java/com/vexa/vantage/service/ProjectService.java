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
    public Project createProject(String name, String description, String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Project project = new Project(name, description, owner);

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

    // Buscar por ID
    public Project findById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));
    }
}