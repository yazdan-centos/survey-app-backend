package org.mapnaom.surveyappbackend.controller;

import lombok.RequiredArgsConstructor;
import org.mapnaom.surveyappbackend.dto.survey.SurveyDashboardResponse;
import org.mapnaom.surveyappbackend.service.SurveyDashboardService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/surveys/dashboard")
@RequiredArgsConstructor
public class SurveyDashboardController {
    private final SurveyDashboardService dashboardService;

    @GetMapping
    public ResponseEntity<SurveyDashboardResponse> getDashboard() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(dashboardService.getDashboard());
    }
}
