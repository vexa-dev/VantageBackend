package com.vexa.vantage.service;

import com.vexa.vantage.model.Story;
import com.vexa.vantage.repository.EpicRepository;
import com.vexa.vantage.repository.StoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StoryService {

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private EpicRepository epicRepository;

    // Crear una historia (Calcula la prioridad automáticamente gracias a la
    // Entidad)
    public Story createStory(Story story) {
        if (story.getEpic() == null) {
            throw new RuntimeException("Story must belong to an Epic");
        }

        // Calculate Story Number within Epic
        Long maxNumber = storyRepository.findMaxStoryNumberByEpicId(story.getEpic().getId()).orElse(0L);
        story.setStoryNumber(maxNumber + 1);

        return storyRepository.save(story);
    }

    // Obtener historias por Epic
    public List<Story> getStoriesByEpic(Long epicId) {
        return storyRepository.findByEpicIdOrderByStoryNumberAsc(epicId);
    }

    // Obtener el BACKLOG INTELIGENTE (Ordenado por WSJF)
    // NOTE: This might be less relevant now that we have Epics, but keeping for
    // backward compatibility or overview
    public List<Story> getPrioritizedBacklog(Long projectId) {
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