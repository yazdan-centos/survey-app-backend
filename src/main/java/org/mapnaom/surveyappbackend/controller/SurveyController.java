package org.mapnaom.surveyappbackend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mapnaom.surveyappbackend.dto.survey.CreateSurveyRequest;
import org.mapnaom.surveyappbackend.dto.survey.UpdateSurveyRequest;
import org.mapnaom.surveyappbackend.dto.survey.SurveyResponseDto;
import org.mapnaom.surveyappbackend.service.SurveyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin
@RestController
@RequestMapping("/api/v1/surveys")
@RequiredArgsConstructor
public class SurveyController {

    private final SurveyService surveyService;

    @PostMapping
    public ResponseEntity<SurveyResponseDto> createSurvey(@Valid @RequestBody CreateSurveyRequest request) {
        return ResponseEntity.ok(SurveyResponseDto.from(surveyService.create(request)));
    }

    @GetMapping
    public ResponseEntity<List<SurveyResponseDto>> getAllSurveys() {
        return ResponseEntity.ok(surveyService.findAll().stream().map(SurveyResponseDto::from).toList());
    }

    @GetMapping("/active")
    public ResponseEntity<SurveyResponseDto> getActiveSurvey() {
        return ResponseEntity.ok(SurveyResponseDto.from(surveyService.findActiveSurvey()));
    }

    @PutMapping("/{surveyId}")
    public ResponseEntity<SurveyResponseDto> updateSurvey(@PathVariable UUID surveyId,
            @Valid @RequestBody UpdateSurveyRequest request) {
        return ResponseEntity.ok(SurveyResponseDto.from(surveyService.update(surveyId, request)));
    }

    @DeleteMapping("/{surveyId}")
    public ResponseEntity<Void> deleteSurvey(@PathVariable UUID surveyId) {
        surveyService.delete(surveyId);
        return ResponseEntity.noContent().build();
    }
}
