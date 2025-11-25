package com.vexa.vantage.repository;

import com.vexa.vantage.model.Epic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EpicRepository extends JpaRepository<Epic, Long> {
    List<Epic> findByProjectId(Long projectId);

    @Query("SELECT MAX(e.epicNumber) FROM Epic e WHERE e.project.id = :projectId")
    Optional<Long> findMaxEpicNumberByProjectId(Long projectId);
}
