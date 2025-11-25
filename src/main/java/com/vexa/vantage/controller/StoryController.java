package com.vexa.vantage.controller;

import com.vexa.vantage.model.Epic;
import com.vexa.vantage.model.Story;
import com.vexa.vantage.model.User;
import com.vexa.vantage.model.Project;
import com.vexa.vantage.repository.ProjectRepository;
import com.vexa.vantage.repository.UserRepository;
import com.vexa.vantage.repository.EpicRepository;
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

    @Autowired
    EpicRepository epicRepository;

    // Crear Historia
    @PostMapping
    public ResponseEntity<?> createStory(@RequestBody StoryRequest request) {
        System.out.println("Creating story for Epic ID: " + request.getEpicId());
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
        story.setAcceptanceCriteria(request.getAcceptanceCriteria());

        story.setProject(project);
        story.setReporter(currentUser); // Quien la crea es el reporter

        // Set Epic if provided
        if (request.getEpicId() != null) {
            Epic epic = epicRepository.findById(request.getEpicId())
                    .orElseThrow(() -> new RuntimeException("Epic no encontrado"));
            story.setEpic(epic);
        } else {
            throw new RuntimeException("La historia debe pertenecer a una épica.");
        }

        Story createdStory = storyService.createStory(story);
        System.out.println(
                "Story created with ID: " + createdStory.getId() + ", Number: " + createdStory.getStoryNumber());
        return ResponseEntity.ok(createdStory);
    }

    // OBTENER BACKLOG PRIORIZADO (La Joya de la Corona 👑)
    @GetMapping("/backlog/{projectId}")
    public List<Story> getBacklog(@PathVariable Long projectId) {
        return storyService.getPrioritizedBacklog(projectId);
    }

    // Get Stories by Epic
    @GetMapping("/epic/{epicId}")
    public List<Story> getStoriesByEpic(@PathVariable Long epicId) {
        System.out.println("Fetching stories for Epic ID: " + epicId);
        List<Story> stories = storyService.getStoriesByEpic(epicId);
        System.out.println("Found " + stories.size() + " stories.");
        stories.forEach(s -> System.out.println(" - Story " + s.getStoryNumber() + ": " + s.getTitle()));
        return stories;
    }

    // Actualizar Puntos (Planning Poker)
    @PatchMapping("/{id}/points")
    public ResponseEntity<?> updatePoints(@PathVariable Long id, @RequestBody Integer points) {
        return ResponseEntity.ok(storyService.updateStoryPoints(id, points));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateStory(@PathVariable Long id, @RequestBody StoryRequest request) {
        Story storyDetails = new Story();
        storyDetails.setTitle(request.getTitle());
        storyDetails.setDescription(request.getDescription());
        storyDetails.setBusinessValue(request.getBusinessValue());
        storyDetails.setUrgency(request.getUrgency());
        storyDetails.setStoryPoints(request.getStoryPoints());
        storyDetails.setAcceptanceCriteria(request.getAcceptanceCriteria());
        return ResponseEntity.ok(storyService.updateStory(id, storyDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStory(@PathVariable Long id) {
        storyService.deleteStory(id);
        return ResponseEntity.ok("Story deleted successfully");
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
    private Long epicId;
    private List<String> acceptanceCriteria;
}