package com.vexa.vantage.service;

import com.vexa.vantage.model.Issue;
import com.vexa.vantage.repository.IssueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IssueService {

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private com.vexa.vantage.repository.StoryRepository storyRepository;

    public List<Issue> getIssuesByStory(Long storyId) {
        return issueRepository.findByStoryId(storyId);
    }

    public java.util.Set<com.vexa.vantage.model.User> getPotentialAssignees(Long storyId) {
        com.vexa.vantage.model.Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story no encontrada"));

        return story.getProject().getMembers().stream()
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> role.getName() == com.vexa.vantage.model.RoleType.ROLE_DEV))
                .collect(java.util.stream.Collectors.toSet());
    }

    public Issue createIssue(Issue issue) {
        return issueRepository.save(issue);
    }

    public Issue updateIssue(Long id, Issue issueDetails) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Issue no encontrado"));

        issue.setTitle(issueDetails.getTitle());
        issue.setDescription(issueDetails.getDescription());
        issue.setStatus(issueDetails.getStatus());
        issue.setCategory(issueDetails.getCategory());
        issue.setTimeEstimate(issueDetails.getTimeEstimate());
        issue.setAssignees(issueDetails.getAssignees());

        return issueRepository.save(issue);
    }

    public void deleteIssue(Long id) {
        issueRepository.deleteById(id);
    }
}
