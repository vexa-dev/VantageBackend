package com.vexa.vantage.controller;

import com.vexa.vantage.model.Sprint;
import com.vexa.vantage.model.Issue;
import com.vexa.vantage.repository.SprintRepository;
import com.vexa.vantage.repository.ProjectRepository;
import com.vexa.vantage.repository.IssueRepository;
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

    @Autowired
    IssueRepository issueRepository;

    // Obtener Sprints de un proyecto
    @GetMapping("/project/{projectId}")
    public List<Sprint> getSprintsByProject(@PathVariable Long projectId) {
        return sprintRepository.findByProjectId(projectId);
    }

    // Obtener Sprint por ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getSprintById(@PathVariable Long id) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sprint no encontrado"));
        return ResponseEntity.ok(sprint);
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

        Sprint savedSprint = sprintRepository.save(sprint);

        if (request.getIssueIds() != null && !request.getIssueIds().isEmpty()) {
            List<Issue> issues = issueRepository.findAllById(request.getIssueIds());
            for (Issue issue : issues) {
                issue.setSprint(savedSprint);
                issueRepository.save(issue);
            }
        }

        return ResponseEntity.ok(savedSprint);
    }

    // Actualizar Sprint
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSprint(@PathVariable Long id, @RequestBody SprintRequest request) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sprint no encontrado"));

        sprint.setName(request.getName());
        sprint.setGoal(request.getGoal());
        sprint.setStartDate(request.getStartDate());
        sprint.setEndDate(request.getEndDate());

        Sprint updatedSprint = sprintRepository.save(sprint);

        // Actualizar issues si se proporcionan
        if (request.getIssueIds() != null) {
            // 1. Obtener issues actuales del sprint
            List<Issue> currentIssues = issueRepository.findByProjectId(sprint.getProject().getId()).stream()
                    .filter(i -> i.getSprint() != null && i.getSprint().getId().equals(sprint.getId()))
                    .collect(java.util.stream.Collectors.toList());

            // 2. Desasignar issues que ya no están en la lista
            for (Issue issue : currentIssues) {
                if (!request.getIssueIds().contains(issue.getId())) {
                    issue.setSprint(null);
                    issueRepository.save(issue);
                }
            }

            // 3. Asignar nuevos issues
            List<Issue> newIssues = issueRepository.findAllById(request.getIssueIds());
            for (Issue issue : newIssues) {
                issue.setSprint(updatedSprint);
                issueRepository.save(issue);
            }
        }

        return ResponseEntity.ok(updatedSprint);
    }

    // Eliminar Sprint
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSprint(@PathVariable Long id) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sprint no encontrado"));

        sprintRepository.delete(sprint);
        return ResponseEntity.ok("Sprint eliminado");
    }
}

@lombok.Data
class SprintRequest {
    private String name;
    private String goal;
    private java.time.LocalDate startDate;
    private java.time.LocalDate endDate;
    private Long projectId;
    private List<Long> issueIds;
}