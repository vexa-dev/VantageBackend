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
@Table(name = "stories")
public class Story {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private StoryStatus status = StoryStatus.BACKLOG;

    // ==========================================
    // CAMPOS DE PRIORIZACIÓN (El Core de tu Proyecto)
    // ==========================================

    // Valor de Negocio (Business Value): 0 a 100
    private Integer businessValue = 0;

    // Urgencia (Time Criticality): 0 a 100
    private Integer urgency = 0;

    // Esfuerzo (Story Points): Fibonacci (1, 2, 3, 5, 8...)
    // Iniciamos en 1 para evitar divisiones por cero
    private Integer storyPoints = 1;

    // SCORE CALCULADO: (Valor + Urgencia) / Esfuerzo
    private Double priorityScore = 0.0;

    // ==========================================
    // RELACIONES
    // ==========================================

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne
    @JoinColumn(name = "epic_id")
    private Epic epic;

    @ManyToOne
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;

    @ManyToOne
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @ManyToOne
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @OneToMany(mappedBy = "story", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "story_acceptance_criteria", joinColumns = @JoinColumn(name = "story_id"))
    @Column(name = "criteria")
    private List<String> acceptanceCriteria = new ArrayList<>();

    @Column(nullable = false)
    private Long storyNumber;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        calculatePriority();
    }

    @PreUpdate
    protected void onUpdate() {
        calculatePriority();
    }

    public void calculatePriority() {
        // Protección contra división por cero
        if (this.storyPoints == null || this.storyPoints <= 0) {
            this.storyPoints = 1;
        }

        double val = (this.businessValue != null) ? this.businessValue : 0;
        double urg = (this.urgency != null) ? this.urgency : 0;

        // Fórmula WSJF Simplificada
        this.priorityScore = (val + urg) / this.storyPoints;
    }
}