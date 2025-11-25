package com.vexa.vantage.service;

import com.vexa.vantage.model.Epic;
import com.vexa.vantage.model.Project;
import com.vexa.vantage.repository.EpicRepository;
import com.vexa.vantage.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EpicService {

    @Autowired
    private EpicRepository epicRepository;

    @Autowired
    private ProjectRepository projectRepository;

    public Epic createEpic(Epic epic, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        epic.setProject(project);

        // Auto-increment Epic Number per Project
        Long maxNumber = epicRepository.findMaxEpicNumberByProjectId(projectId).orElse(0L);
        epic.setEpicNumber(maxNumber + 1);

        return epicRepository.save(epic);
    }

    public List<Epic> getEpicsByProject(Long projectId) {
        return epicRepository.findByProjectId(projectId);
    }

    public Epic getEpicById(Long epicId) {
        return epicRepository.findById(epicId)
                .orElseThrow(() -> new RuntimeException("Epic not found"));
    }
}
