package org.mapnaom.surveyappbackend.security;

import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.Test;
import org.mapnaom.surveyappbackend.controller.UserController;
import org.mapnaom.surveyappbackend.controller.SurveyController;
import org.mapnaom.surveyappbackend.entity.Survey;
import org.mapnaom.surveyappbackend.repository.UserRepository;
import org.mapnaom.surveyappbackend.service.UserExcelService;
import org.mapnaom.surveyappbackend.service.UserService;
import org.mapnaom.surveyappbackend.service.SurveyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({UserController.class, SurveyController.class})
@Import(SecurityConfig.class)
class UserApiSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean UserService service;
    @MockitoBean UserExcelService excel;
    @MockitoBean UserRepository repository;
    @MockitoBean JwtService jwtService;
    @MockitoBean SurveyService surveyService;

    @Test
    void usersAndAdminsCanAccessActiveSurvey() throws Exception {
        Survey survey = new Survey();
        survey.setId(10032L);
        survey.setTitle("Current survey");
        survey.setVersion("2026");
        survey.setActive(true);
        when(surveyService.findActiveSurvey()).thenReturn(survey);
        for (String role : List.of("USER", "ADMIN", "SURVEY_ADMIN")) {
            mvc.perform(get("/api/v1/surveys/active").with(user("caller").roles(role)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void missingActiveSurveyReturnsNotFoundForUser() throws Exception {
        when(surveyService.findActiveSurvey())
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "No active survey found"));
        mvc.perform(get("/api/v1/surveys/active").with(user("caller").roles("USER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void activeSurveyRejectsUnauthenticatedRequestsAndUnsupportedRoles() throws Exception {
        mvc.perform(get("/api/v1/surveys/active")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/surveys/active").with(user("caller").roles("OTHER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(surveyService);
    }

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mvc.perform(get("/api/users")).andExpect(status().is4xxClientError());
        verifyNoInteractions(service);
    }

    @Test
    void errorDispatchDoesNotReplaceAnEndpointErrorWithUnauthorized() throws Exception {
        mvc.perform(get("/error").with(request -> {
            request.setDispatcherType(DispatcherType.ERROR);
            return request;
        })).andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(401, result.getResponse().getStatus()));
    }

    @Test
    void nonAdminsCannotAccessAnyUserManagementEndpoint() throws Exception {
        Long id = 10033L;
        for (String role : List.of("USER", "SURVEY_ADMIN")) {
            for (String path : List.of("/api/users", "/api/users/" + id, "/api/users/search", "/api/users/template")) {
                mvc.perform(get(path).with(user("caller").roles(role))).andExpect(status().isForbidden());
            }
            mvc.perform(put("/api/users/{id}", id).with(user("caller").roles(role))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"alice\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(delete("/api/users/{id}", id).with(user("caller").roles(role)))
                    .andExpect(status().isForbidden());
            mvc.perform(multipart("/api/users/import").with(user("caller").roles(role)))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(service, excel);
    }

    @Test
    void adminCanListAndDeleteUsers() throws Exception {
        when(service.findAll()).thenReturn(List.of());
        mvc.perform(get("/api/users").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        Long id = 10034L;
        mvc.perform(delete("/api/users/{id}", id).with(user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());
        verify(service).delete(id);
    }
}
