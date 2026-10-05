package org.mapnaom.surveyappbackend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mapnaom.surveyappbackend.dto.assignment.SaveSurveyAssignmentRequest;
import org.mapnaom.surveyappbackend.dto.assignment.SurveyAssignmentResponseDto;
import org.mapnaom.surveyappbackend.service.SurveyAssignmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/survey-assignments")
@RequiredArgsConstructor
public class SurveyAssignmentController {

    private final SurveyAssignmentService surveyAssignmentService;

    /** Lists persisted assignment history for one survey, including user details. */
    @GetMapping
    public ResponseEntity<List<SurveyAssignmentResponseDto>> listAssignments(@RequestParam Long surveyId) {
        return ResponseEntity.ok(surveyAssignmentService.findAssignmentsForSurvey(surveyId));
    }

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
    public ResponseEntity<SurveyAssignmentResponseDto> revokeAssignment(@PathVariable Long assignmentId) {
        return ResponseEntity.ok(surveyAssignmentService.revoke(assignmentId));
    }
}
