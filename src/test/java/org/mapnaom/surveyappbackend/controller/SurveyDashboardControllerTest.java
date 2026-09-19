package org.mapnaom.surveyappbackend.controller;

import org.junit.jupiter.api.Test;
import org.mapnaom.surveyappbackend.dto.survey.SurveyDashboardResponse;
import org.mapnaom.surveyappbackend.dto.survey.SurveyDashboardResponse.*;
import org.mapnaom.surveyappbackend.entity.SurveyRole;
import org.mapnaom.surveyappbackend.repository.UserRepository;
import org.mapnaom.surveyappbackend.security.JwtService;
import org.mapnaom.surveyappbackend.security.SecurityConfig;
import org.mapnaom.surveyappbackend.service.SurveyDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SurveyDashboardController.class)
@Import(SecurityConfig.class)
class SurveyDashboardControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean SurveyDashboardService service;
    @MockitoBean UserRepository users;
    @MockitoBean JwtService jwtService;

    @Test
    void surveyAdminsAndAdminsCanReadDashboardJson() throws Exception {
        var now = Instant.parse("2026-09-18T10:00:00Z");
        var id = UUID.randomUUID();
        var zero = BigDecimal.ZERO.setScale(2);
        var survey = new SurveyStats(id, "Annual survey", "v1", true, now, now,
                1, 1, 1, 1, 0, zero, now,
                List.of(new AudienceStats(SurveyRole.BOARD, 1, 1, 1, 0, zero)));
        when(service.getDashboard()).thenReturn(new SurveyDashboardResponse(now,
                new Summary(1, 1, 0, 1, 1, 0, 1, 1, 0, zero), List.of(survey)));

        for (String role : List.of("SURVEY_ADMIN", "ADMIN")) {
            mvc.perform(get("/api/surveys/dashboard").with(user("reviewer").roles(role)))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(jsonPath("$.summary.totalSurveys").value(1))
                    .andExpect(jsonPath("$.summary.totalResponses").value(1))
                    .andExpect(jsonPath("$.surveys[0].id").value(id.toString()))
                    .andExpect(jsonPath("$.surveys[0].audiences[0].role").value("BOARD"))
                    .andExpect(jsonPath("$.surveys[0].respondentUsername").doesNotExist())
                    .andExpect(jsonPath("$.surveys[0].answers").doesNotExist());
        }
    }

    @Test
    void ordinaryUsersAreForbidden() throws Exception {
        mvc.perform(get("/api/surveys/dashboard").with(user("respondent").roles("USER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void anonymousRequestsAreRejected() throws Exception {
        mvc.perform(get("/api/surveys/dashboard")).andExpect(status().is4xxClientError());
        verifyNoInteractions(service);
    }
}
