package com.vexa.vantage.repository;

import com.vexa.vantage.model.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StoryRepository extends JpaRepository<Story, Long> {

    // CORRECCIÓN: Agregamos (Long projectId) dentro del paréntesis
    List<Story> findByProjectIdAndSprintIsNullOrderByPriorityScoreDesc(Long projectId);

    List<Story> findBySprintId(Long sprintId);

    List<Story> findByAssigneeId(Long userId);
}