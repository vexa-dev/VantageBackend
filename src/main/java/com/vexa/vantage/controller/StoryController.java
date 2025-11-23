package com.vexa.vantage.controller;

import com.vexa.vantage.model.Project;
import com.vexa.vantage.model.Story;
import com.vexa.vantage.model.User;
import com.vexa.vantage.repository.ProjectRepository;
import com.vexa.vantage.repository.UserRepository;
import com.vexa.vantage.service.StoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/stories")
public class StoryController {

    @Autowired
    StoryService storyService;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    UserRepository userRepository;

    // Crear Historia
    @PostMapping
    public ResponseEntity<?> createStory(@RequestBody StoryRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User currentUser = userRepository.findByEmail(email).orElseThrow();

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

        Story story = new Story();
        story.setTitle(request.getTitle());
        story.setDescription(request.getDescription());
        story.setBusinessValue(request.getBusinessValue());
        story.setUrgency(request.getUrgency());
        story.setStoryPoints(request.getStoryPoints());

        story.setProject(project);
        story.setReporter(currentUser); // Quien la crea es el reporter

        Story createdStory = storyService.createStory(story);
        return ResponseEntity.ok(createdStory);
    }

    // OBTENER BACKLOG PRIORIZADO (La Joya de la Corona 👑)
    @GetMapping("/backlog/{projectId}")
    public List<Story> getBacklog(@PathVariable Long projectId) {
        // Esto devuelve la lista ya ordenada por la fórmula matemática
        return storyService.getPrioritizedBacklog(projectId);
    }

    // Actualizar Puntos (Planning Poker)
    @PatchMapping("/{id}/points")
    public ResponseEntity<?> updatePoints(@PathVariable Long id, @RequestBody Integer points) {
        return ResponseEntity.ok(storyService.updateStoryPoints(id, points));
    }
}

// DTO simple para recibir datos limpios
@lombok.Data
class StoryRequest {
    private String title;
    private String description;
    private Integer businessValue;
    private Integer urgency;
    private Integer storyPoints;
    private Long projectId;
}