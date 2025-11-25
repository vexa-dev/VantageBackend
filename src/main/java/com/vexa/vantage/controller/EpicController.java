package com.vexa.vantage.controller;

import com.vexa.vantage.model.Epic;
import com.vexa.vantage.service.EpicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/epics")
public class EpicController {

    @Autowired
    private EpicService epicService;

    @PostMapping
    public ResponseEntity<?> createEpic(@RequestBody EpicRequest request) {
        Epic epic = new Epic();
        epic.setTitle(request.getTitle());
        epic.setDescription(request.getDescription());

        return ResponseEntity.ok(epicService.createEpic(epic, request.getProjectId()));
    }

    @GetMapping("/project/{projectId}")
    public List<Epic> getEpicsByProject(@PathVariable Long projectId) {
        return epicService.getEpicsByProject(projectId);
    }
}

@lombok.Data
class EpicRequest {
    private String title;
    private String description;
    private Long projectId;
}
