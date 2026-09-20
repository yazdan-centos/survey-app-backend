package org.mapnaom.surveyappbackend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mapnaom.surveyappbackend.dto.survey.CreateSurveyRequest;
import org.mapnaom.surveyappbackend.dto.survey.UpdateSurveyRequest;
import org.mapnaom.surveyappbackend.entity.Survey;
import org.mapnaom.surveyappbackend.service.SurveyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin
@RestController
@RequestMapping("/api/surveys")
@RequiredArgsConstructor
public class SurveyController {

    private final SurveyService surveyService;

    @PostMapping
    public ResponseEntity<Survey> create(@Valid @RequestBody CreateSurveyRequest request) {
        return ResponseEntity.ok(surveyService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<Survey>> getAll() {
        return ResponseEntity.ok(surveyService.findAll());
    }

    @GetMapping("/active")
    public ResponseEntity<Survey> getActiveSurvey() {
        return ResponseEntity.ok(surveyService.findActiveSurvey());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Survey> update(@PathVariable UUID id,
                                         @Valid @RequestBody UpdateSurveyRequest request) {
        return ResponseEntity.ok(surveyService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        surveyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
