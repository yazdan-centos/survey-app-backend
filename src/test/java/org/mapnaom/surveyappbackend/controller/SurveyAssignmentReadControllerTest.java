package org.mapnaom.surveyappbackend.controller;

import org.junit.jupiter.api.Test;
import org.mapnaom.surveyappbackend.dto.assignment.SurveyAssignmentResponseDto;
import org.mapnaom.surveyappbackend.dto.user.UserResponse;
import org.mapnaom.surveyappbackend.entity.User;
import org.mapnaom.surveyappbackend.repository.UserRepository;
import org.mapnaom.surveyappbackend.security.JwtService;
import org.mapnaom.surveyappbackend.security.SecurityConfig;
import org.mapnaom.surveyappbackend.service.SurveyAssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SurveyAssignmentController.class)
@Import(SecurityConfig.class)
class SurveyAssignmentReadControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean SurveyAssignmentService service;
    @MockitoBean UserRepository users;
    @MockitoBean JwtService jwtService;

    @Test
    void adminsReceiveAssignmentIdsAndUserDetailsWithoutCredentialsOrCycles() throws Exception {
        User assignedUser = new User();
        assignedUser.setId(7L);
        assignedUser.setUsername("assigned-user");
        assignedUser.setPassword("must-not-be-serialized");
        when(service.findAssignmentsForSurvey(42L)).thenReturn(List.of(
                SurveyAssignmentResponseDto.builder().id(9L).user(UserResponse.from(assignedUser)).build()));
        for (String role : List.of("ADMIN", "SURVEY_ADMIN")) {
            mvc.perform(get("/api/survey-assignments").param("surveyId", "42").with(user("admin").roles(role)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(9))
                    .andExpect(jsonPath("$[0].user.username").value("assigned-user"))
                    .andExpect(jsonPath("$[0].user.password").doesNotExist())
                    .andExpect(jsonPath("$[0].user.surveyAssignments").doesNotExist());
        }
    }

    @Test
    void listIsNotAvailableToAnonymousOrRegularUsers() throws Exception {
        mvc.perform(get("/api/survey-assignments").param("surveyId", "42"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/survey-assignments").param("surveyId", "42").with(user("member").roles("USER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void requiresSurveyFilterAndDistinguishesEmptyFromMissing() throws Exception {
        mvc.perform(get("/api/survey-assignments").with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/survey-assignments").param("surveyId", "invalid").with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());
        when(service.findAssignmentsForSurvey(42L)).thenReturn(List.of());
        mvc.perform(get("/api/survey-assignments").param("surveyId", "42").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        when(service.findAssignmentsForSurvey(99L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        mvc.perform(get("/api/survey-assignments").param("surveyId", "99").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }
}
