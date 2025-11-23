package com.vexa.vantage.repository;

import com.vexa.vantage.model.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SprintRepository extends JpaRepository<Sprint, Long> {
    // Listar sprints de un proyecto
    List<Sprint> findByProjectId(Long projectId);

    // Buscar el sprint activo de un proyecto
    List<Sprint> findByProjectIdAndActiveTrue(Long projectId);
}