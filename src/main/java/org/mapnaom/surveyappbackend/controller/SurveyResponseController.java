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

@RestController
@RequestMapping("/api/survey-responses")
@RequiredArgsConstructor
public class SurveyResponseController {
    private final SurveyResponseService responseService;

    @PostMapping
    public ResponseEntity<SurveyResponseDetails> create(@Valid @RequestBody SaveSurveyResponseRequest request) {
        var response = responseService.create(request);
        return ResponseEntity.created(URI.create("/api/survey-responses/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SurveyResponseDetails> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(responseService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SurveyResponseDetails> update(@PathVariable UUID id,
            @Valid @RequestBody SaveSurveyResponseRequest request) {
        return ResponseEntity.ok(responseService.update(id, request));
    }
}
