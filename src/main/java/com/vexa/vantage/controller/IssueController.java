package com.vexa.vantage.controller;

import com.vexa.vantage.model.*;
import com.vexa.vantage.repository.StoryRepository;
import com.vexa.vantage.repository.UserRepository;
import com.vexa.vantage.service.IssueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/issues")
public class IssueController {

    @Autowired
    private IssueService issueService;

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/story/{storyId}")
    public List<Issue> getIssuesByStory(@PathVariable Long storyId) {
        return issueService.getIssuesByStory(storyId);
    }

    @GetMapping("/story/{storyId}/assignees")
    public ResponseEntity<?> getPotentialAssignees(@PathVariable Long storyId) {
        return ResponseEntity.ok(issueService.getPotentialAssignees(storyId));
    }

    @GetMapping("/project/{projectId}/unassigned")
    public ResponseEntity<?> getUnassignedIssues(@PathVariable Long projectId) {
        return ResponseEntity.ok(issueService.getUnassignedIssues(projectId));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<?> getIssuesByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(issueService.getIssuesByProject(projectId));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createIssue(@RequestBody IssueRequest request) {
        Story story = storyRepository.findById(request.getStoryId())
                .orElseThrow(() -> new RuntimeException("Story no encontrada"));

        Issue issue = new Issue();
        issue.setTitle(request.getTitle());
        issue.setDescription(request.getDescription());
        issue.setStatus(request.getStatus());
        issue.setCategory(request.getCategory());
        issue.setTimeEstimate(request.getTimeEstimate());
        issue.setStory(story);

        if (request.getAssigneeIds() != null && !request.getAssigneeIds().isEmpty()) {
            Set<User> assignees = new HashSet<>(userRepository.findAllById(request.getAssigneeIds()));
            issue.setAssignees(assignees);
        }

        return ResponseEntity.ok(issueService.createIssue(issue));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SM') or hasRole('SCRUM_MASTER')")
    public ResponseEntity<?> updateIssue(@PathVariable Long id, @RequestBody IssueRequest request) {
        Issue issueDetails = new Issue();
        issueDetails.setTitle(request.getTitle());
        issueDetails.setDescription(request.getDescription());
        issueDetails.setStatus(request.getStatus());
        issueDetails.setCategory(request.getCategory());
        issueDetails.setTimeEstimate(request.getTimeEstimate());

        if (request.getAssigneeIds() != null) {
            Set<User> assignees = new HashSet<>(userRepository.findAllById(request.getAssigneeIds()));
            issueDetails.setAssignees(assignees);
        }

        return ResponseEntity.ok(issueService.updateIssue(id, issueDetails));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateIssueStatus(@PathVariable Long id,
            @RequestBody java.util.Map<String, String> statusUpdate) {
        try {
            IssueStatus status = IssueStatus.valueOf(statusUpdate.get("status"));
            return ResponseEntity.ok(issueService.updateIssueStatus(id, status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Estado inválido");
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SM') or hasRole('SCRUM_MASTER')")
    public ResponseEntity<?> deleteIssue(@PathVariable Long id) {
        issueService.deleteIssue(id);
        return ResponseEntity.ok("Issue eliminado");
    }
}

@lombok.Data
class IssueRequest {
    private String title;
    private String description;
    private Double timeEstimate;
    private IssueStatus status;
    private IssueCategory category;
    private Long storyId;
    private List<Long> assigneeIds;
}
