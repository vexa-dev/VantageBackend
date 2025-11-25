package com.vexa.vantage.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "epics")
public class Epic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Long epicNumber; // Unique number within the project (e.g., Epic 1, Epic 2)

    @Enumerated(EnumType.STRING)
    private StoryStatus status = StoryStatus.BACKLOG; // Reusing StoryStatus for simplicity or create EpicStatus

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "epic", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Story> stories = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
