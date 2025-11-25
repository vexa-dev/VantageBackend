package com.vexa.vantage.repository;

import com.vexa.vantage.model.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface StoryRepository extends JpaRepository<Story, Long> {

    List<Story> findByProjectIdAndSprintIsNullOrderByPriorityScoreDesc(Long projectId);

    List<Story> findBySprintId(Long sprintId);

    List<Story> findByAssigneeId(Long userId);

    List<Story> findByEpicIdOrderByStoryNumberAsc(Long epicId);

    @Query("SELECT MAX(s.storyNumber) FROM Story s WHERE s.epic.id = :epicId")
    Optional<Long> findMaxStoryNumberByEpicId(Long epicId);
}