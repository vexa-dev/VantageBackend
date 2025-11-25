package com.vexa.vantage.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@Entity
@Table(name = "issues")
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "time_estimate")
    private Double timeEstimate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private IssueStatus status = IssueStatus.TO_DO;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private IssueCategory category = IssueCategory.BACKEND;

    @ManyToOne
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @ManyToMany
    @JoinTable(name = "issue_assignees", joinColumns = @JoinColumn(name = "issue_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> assignees = new HashSet<>();

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
