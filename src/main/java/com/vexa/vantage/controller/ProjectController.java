package com.vexa.vantage.controller;

import com.vexa.vantage.model.Project;
import com.vexa.vantage.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired
    ProjectService projectService;

    // Obtener MIS proyectos (donde soy dueño o miembro)
    @GetMapping
    public List<Project> getMyProjects() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName(); // El email del usuario logueado
        return projectService.getMyProjects(email);
    }

    // Crear un nuevo proyecto
    @PostMapping
    public ResponseEntity<?> createProject(@RequestBody Map<String, String> request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String ownerEmail = auth.getName();

        String name = request.get("name");
        String description = request.get("description");

        Project newProject = projectService.createProject(name, description, ownerEmail);
        return ResponseEntity.ok(newProject);
    }

    // Obtener un proyecto por ID (Para ver el detalle)
    @GetMapping("/{id}")
    public ResponseEntity<?> getProject(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.findById(id));
    }
}