package com.vexa.vantage.repository;

import com.vexa.vantage.model.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, Long> {
    List<Issue> findByStoryId(Long storyId);

    @Query("SELECT i FROM Issue i WHERE i.story.epic.project.id = :projectId AND i.sprint IS NULL")
    List<Issue> findUnassignedIssuesByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT i FROM Issue i WHERE i.story.epic.project.id = :projectId")
    List<Issue> findByProjectId(@Param("projectId") Long projectId);
}
