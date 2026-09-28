package org.mapnaom.surveyappbackend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mapnaom.surveyappbackend.dto.assignment.SaveSurveyAssignmentRequest;
import org.mapnaom.surveyappbackend.dto.assignment.SurveyAssignmentResponseDto;
import org.mapnaom.surveyappbackend.service.SurveyAssignmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin
@RestController
@RequestMapping("/api/survey-assignments")
@RequiredArgsConstructor
public class SurveyAssignmentController {

    private final SurveyAssignmentService surveyAssignmentService;

    /**
     * Assigns the given surveys to the given users (cartesian product).
     * Idempotent for existing (user, survey) pairs.
     */
    @PostMapping
    public ResponseEntity<List<SurveyAssignmentResponseDto>> assignSurveys(
            @Valid @RequestBody SaveSurveyAssignmentRequest request) {
        return ResponseEntity.ok(surveyAssignmentService.assign(request));
    }

    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<SurveyAssignmentResponseDto> revokeAssignment(@PathVariable UUID assignmentId) {
        return ResponseEntity.ok(surveyAssignmentService.revoke(assignmentId));
    }
}
