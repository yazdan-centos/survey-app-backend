package org.mapnaom.surveyappbackend.controller;

import lombok.RequiredArgsConstructor;
import org.mapnaom.surveyappbackend.dto.survey.SurveyDimensionResultsResponse;
import org.mapnaom.surveyappbackend.entity.SurveyRole;
import org.mapnaom.surveyappbackend.service.SurveyDimensionResultsService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@CrossOrigin
@RestController
@RequestMapping("/api/surveys")
@RequiredArgsConstructor
public class SurveyDimensionResultsController {
    private final SurveyDimensionResultsService service;

    @GetMapping("/results")
    public ResponseEntity<SurveyDimensionResultsResponse> getResults(
            @RequestParam(required = false) Long surveyId,
            @RequestParam(required = false) SurveyRole role) {
        return response(service.getResults(surveyId, role));
    }

    @GetMapping("/{surveyId}/results")
    public ResponseEntity<SurveyDimensionResultsResponse> getSurveyResults(
            @PathVariable Long surveyId,
            @RequestParam(required = false) SurveyRole role) {
        return response(service.getResults(surveyId, role));
    }

    private ResponseEntity<SurveyDimensionResultsResponse> response(SurveyDimensionResultsResponse results) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(results);
    }
}
