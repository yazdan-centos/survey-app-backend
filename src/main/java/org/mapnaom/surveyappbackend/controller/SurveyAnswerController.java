package org.mapnaom.surveyappbackend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mapnaom.surveyappbackend.dto.response.SaveSurveyAnswerRequest;
import org.mapnaom.surveyappbackend.dto.response.SurveyAnswerDetails;
import org.mapnaom.surveyappbackend.service.SurveyAnswerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@CrossOrigin
@RestController
@RequestMapping("/api/survey-responses/{responseId}/answers")
@RequiredArgsConstructor
public class SurveyAnswerController {
    private final SurveyAnswerService surveyAnswerService;

    @PostMapping
    public ResponseEntity<SurveyAnswerDetails> createSurveyAnswer(@PathVariable UUID responseId,
            @Valid @RequestBody SaveSurveyAnswerRequest request) {
        var surveyAnswer = surveyAnswerService.create(responseId, request);
        return ResponseEntity.created(
                        URI.create("/api/survey-responses/" + responseId + "/answers/" + surveyAnswer.id()))
                .body(surveyAnswer);
    }

    @GetMapping
    public ResponseEntity<List<SurveyAnswerDetails>> getSurveyAnswersByResponseId(@PathVariable UUID responseId) {
        return ResponseEntity.ok(surveyAnswerService.findByResponseId(responseId));
    }

    @GetMapping("/{answerId}")
    public ResponseEntity<SurveyAnswerDetails> getSurveyAnswerById(@PathVariable UUID responseId,
            @PathVariable UUID answerId) {
        return ResponseEntity.ok(surveyAnswerService.findById(responseId, answerId));
    }

    @PutMapping("/{answerId}")
    public ResponseEntity<SurveyAnswerDetails> updateSurveyAnswer(@PathVariable UUID responseId,
            @PathVariable UUID answerId,
            @Valid @RequestBody SaveSurveyAnswerRequest request) {
        return ResponseEntity.ok(surveyAnswerService.update(responseId, answerId, request));
    }
}
