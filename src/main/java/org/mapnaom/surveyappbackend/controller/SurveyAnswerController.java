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
    private final SurveyAnswerService answerService;

    @PostMapping
    public ResponseEntity<SurveyAnswerDetails> create(@PathVariable UUID responseId,
            @Valid @RequestBody SaveSurveyAnswerRequest request) {
        var answer = answerService.create(responseId, request);
        return ResponseEntity.created(URI.create("/api/survey-responses/" + responseId + "/answers/" + answer.id()))
                .body(answer);
    }

    @GetMapping
    public ResponseEntity<List<SurveyAnswerDetails>> findByResponseId(@PathVariable UUID responseId) {
        return ResponseEntity.ok(answerService.findByResponseId(responseId));
    }

    @GetMapping("/{answerId}")
    public ResponseEntity<SurveyAnswerDetails> findById(@PathVariable UUID responseId, @PathVariable UUID answerId) {
        return ResponseEntity.ok(answerService.findById(responseId, answerId));
    }

    @PutMapping("/{answerId}")
    public ResponseEntity<SurveyAnswerDetails> update(@PathVariable UUID responseId, @PathVariable UUID answerId,
            @Valid @RequestBody SaveSurveyAnswerRequest request) {
        return ResponseEntity.ok(answerService.update(responseId, answerId, request));
    }
}
