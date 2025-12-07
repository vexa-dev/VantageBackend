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

    public Story updateStory(Long id, Story storyDetails) {
        Story story = storyRepository.findById(id).orElseThrow(() -> new RuntimeException("Story not found"));
        story.setTitle(storyDetails.getTitle());
        story.setDescription(storyDetails.getDescription());
        story.setBusinessValue(storyDetails.getBusinessValue());
        story.setUrgency(storyDetails.getUrgency());
        story.setStoryPoints(storyDetails.getStoryPoints());
        story.setTshirtSize(storyDetails.getTshirtSize());

        // Update acceptance criteria
        if (storyDetails.getAcceptanceCriteria() != null) {
            story.getAcceptanceCriteria().clear();
            story.getAcceptanceCriteria().addAll(storyDetails.getAcceptanceCriteria());
        }

        return storyRepository.save(story);
    }

    public void deleteStory(Long id) {
        Story story = storyRepository.findById(id).orElseThrow(() -> new RuntimeException("Story not found"));
        storyRepository.delete(story);
    }

    public Story getStoryById(Long id) {
        return storyRepository.findById(id).orElseThrow(() -> new RuntimeException("Story not found"));
    }
}