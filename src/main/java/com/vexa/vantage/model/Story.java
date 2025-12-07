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
    // Iniciamos en null hasta el Planning
    private Integer storyPoints;

    @Enumerated(EnumType.STRING)
    private TShirtSize tshirtSize;

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
        // Usar variable local para cálculo, no sobrescribir el campo si es null
        double effectivePoints = 1.0;

        if (this.storyPoints != null && this.storyPoints > 0) {
            effectivePoints = this.storyPoints;
        } else if (this.tshirtSize != null) {
            // Estimar puntos provisionales basados en talla para el cálculo del score
            switch (this.tshirtSize) {
                case XS:
                    effectivePoints = 1.0;
                    break;
                case S:
                    effectivePoints = 2.0;
                    break;
                case M:
                    effectivePoints = 5.0;
                    break;
                case L:
                    effectivePoints = 8.0;
                    break;
                case XL:
                    effectivePoints = 13.0;
                    break;
                default:
                    effectivePoints = 1.0;
            }
        }

        double val = (this.businessValue != null) ? this.businessValue : 0;
        double urg = (this.urgency != null) ? this.urgency : 0;

        // Fórmula WSJF Simplificada
        this.priorityScore = (val + urg) / effectivePoints;
    }
}