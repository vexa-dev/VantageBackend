package com.vexa.vantage.repository;

import com.vexa.vantage.model.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, Long> {
    List<Issue> findByStoryId(Long storyId);
}
