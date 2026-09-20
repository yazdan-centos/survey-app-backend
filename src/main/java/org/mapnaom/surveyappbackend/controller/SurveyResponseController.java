package org.mapnaom.surveyappbackend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mapnaom.surveyappbackend.dto.response.SaveSurveyResponseRequest;
import org.mapnaom.surveyappbackend.dto.response.SurveyResponseDetails;
import org.mapnaom.surveyappbackend.service.SurveyResponseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@CrossOrigin
@RestController
@RequestMapping("/api/survey-responses")
@RequiredArgsConstructor
public class SurveyResponseController {
    private final SurveyResponseService surveyResponseService;

    @PostMapping
    public ResponseEntity<SurveyResponseDetails> createSurveyResponse(
            @Valid @RequestBody SaveSurveyResponseRequest request) {
        var surveyResponse = surveyResponseService.create(request);
        return ResponseEntity.created(URI.create("/api/survey-responses/" + surveyResponse.id()))
                .body(surveyResponse);
    }

    @GetMapping("/{responseId}")
    public ResponseEntity<SurveyResponseDetails> getSurveyResponseById(@PathVariable UUID responseId) {
        return ResponseEntity.ok(surveyResponseService.findById(responseId));
    }

    @PutMapping("/{responseId}")
    public ResponseEntity<SurveyResponseDetails> updateSurveyResponse(@PathVariable UUID responseId,
            @Valid @RequestBody SaveSurveyResponseRequest request) {
        return ResponseEntity.ok(surveyResponseService.update(responseId, request));
    }
}
