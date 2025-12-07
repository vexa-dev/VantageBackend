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

    // Obtener TODOS los proyectos (para el directorio)
    @GetMapping("/all")
    public List<Project> getAllProjects() {
        return projectService.getAllProjects();
    }

    // Crear un nuevo proyecto
    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createProject(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        String icon = (String) request.get("icon");

        java.time.LocalDate startDate = null;
        if (request.get("startDate") != null) {
            startDate = java.time.LocalDate.parse((String) request.get("startDate"));
        }

        java.time.LocalDate endDate = null;
        if (request.get("endDate") != null) {
            endDate = java.time.LocalDate.parse((String) request.get("endDate"));
        }

        Long poId = null;
        if (request.get("poId") != null) {
            poId = Long.valueOf(request.get("poId").toString());
        }

        Long smId = null;
        if (request.get("smId") != null) {
            smId = Long.valueOf(request.get("smId").toString());
        }

        List<Long> devIds = null;
        if (request.get("devIds") != null) {
            devIds = (List<Long>) request.get("devIds");
            // Ensure types are correct if JSON library parses as Integers
            devIds = devIds.stream().map(id -> Long.valueOf(id.toString())).toList();
        }

        Project newProject = projectService.createProject(name, description, icon, poId, smId, devIds, startDate,
                endDate);
        return ResponseEntity.ok(newProject);
    }

    // Obtener un proyecto por ID (Para ver el detalle)
    @GetMapping("/{id}")
    public ResponseEntity<?> getProject(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.findById(id));
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateProject(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        String icon = (String) request.get("icon");
        String status = (String) request.get("status");

        java.time.LocalDate startDate = null;
        if (request.get("startDate") != null) {
            startDate = java.time.LocalDate.parse((String) request.get("startDate"));
        }

        java.time.LocalDate endDate = null;
        if (request.get("endDate") != null) {
            endDate = java.time.LocalDate.parse((String) request.get("endDate"));
        }

        Long poId = null;
        if (request.get("poId") != null) {
            poId = Long.valueOf(request.get("poId").toString());
        }

        Long smId = null;
        if (request.get("scrumMasterId") != null) { // Frontend sends as scrumMasterId usually
            smId = Long.valueOf(request.get("scrumMasterId").toString());
        } else if (request.get("smId") != null) {
            smId = Long.valueOf(request.get("smId").toString());
        }

        List<Long> devIds = null;
        if (request.get("devIds") != null) {
            devIds = (List<Long>) request.get("devIds");
            devIds = devIds.stream().map(val -> Long.valueOf(val.toString())).toList();
        }

        return ResponseEntity.ok(projectService.updateProject(id, name, description, icon, startDate, endDate, status,
                poId, smId, devIds));
    }

    @PutMapping("/{id}/members")
    public ResponseEntity<?> updateMembers(@PathVariable Long id, @RequestBody List<Long> userIds) {
        return ResponseEntity.ok(projectService.updateMembers(id, userIds));
    }
}