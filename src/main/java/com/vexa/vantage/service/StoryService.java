package com.vexa.vantage.service;

import com.vexa.vantage.model.Story;
import com.vexa.vantage.repository.StoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StoryService {

    @Autowired
    private StoryRepository storyRepository;

    // Crear una historia (Calcula la prioridad automáticamente gracias a la
    // Entidad)
    public Story createStory(Story story) {
        return storyRepository.save(story);
    }

    // Obtener el BACKLOG INTELIGENTE (Ordenado por WSJF)
    public List<Story> getPrioritizedBacklog(Long projectId) {
        // CORRECCIÓN: Ahora le pasamos la variable 'projectId' al repositorio
        return storyRepository.findByProjectIdAndSprintIsNullOrderByPriorityScoreDesc(projectId);
    }

    // Obtener historias de un Sprint activo (Tablero Kanban)
    public List<Story> getSprintStories(Long sprintId) {
        return storyRepository.findBySprintId(sprintId);
    }

    // Votar / Actualizar puntos (Recalcula prioridad)
    public Story updateStoryPoints(Long storyId, Integer points) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Historia no encontrada"));

        story.setStoryPoints(points);
        // Al guardar, el @PreUpdate de la entidad recalcula el Score
        return storyRepository.save(story);
    }
}