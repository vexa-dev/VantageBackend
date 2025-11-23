package com.vexa.vantage.controller;

import com.vexa.vantage.model.Sprint;
import com.vexa.vantage.repository.SprintRepository;
import com.vexa.vantage.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/sprints")
public class SprintController {

    @Autowired
    SprintRepository sprintRepository;

    @Autowired
    ProjectRepository projectRepository;

    // Obtener Sprints de un proyecto
    @GetMapping("/project/{projectId}")
    public List<Sprint> getSprintsByProject(@PathVariable Long projectId) {
        return sprintRepository.findByProjectId(projectId);
    }

    // Crear Sprint
    @PostMapping
    public ResponseEntity<?> createSprint(@RequestBody SprintRequest request) {
        Sprint sprint = new Sprint();
        sprint.setName(request.getName());
        sprint.setGoal(request.getGoal());
        sprint.setStartDate(request.getStartDate());
        sprint.setEndDate(request.getEndDate());

        sprint.setProject(projectRepository.findById(request.getProjectId()).orElseThrow());

        return ResponseEntity.ok(sprintRepository.save(sprint));
    }
}

@lombok.Data
class SprintRequest {
    private String name;
    private String goal;
    private java.time.LocalDate startDate;
    private java.time.LocalDate endDate;
    private Long projectId;
}