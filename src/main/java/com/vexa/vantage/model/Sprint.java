package com.vexa.vantage.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "sprints")
public class Sprint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name; // Ej: "Sprint 1"

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(length = 255)
    private String goal; // Objetivo del Sprint

    private Boolean active = false; // True = Es el sprint actual

    // Un Sprint pertenece a un Proyecto
    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @OneToMany(mappedBy = "sprint")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties("sprint")
    private List<Issue> issues;
}