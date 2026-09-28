package org.mapnaom.surveyappbackend.security;

import org.junit.jupiter.api.Test;
import org.mapnaom.surveyappbackend.controller.QuestionController;
import org.mapnaom.surveyappbackend.repository.UserRepository;
import org.mapnaom.surveyappbackend.service.QuestionExcelService;
import org.mapnaom.surveyappbackend.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QuestionController.class)
@Import(SecurityConfig.class)
class QuestionSearchSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean QuestionService service;
    @MockitoBean QuestionExcelService excel;
    @MockitoBean UserRepository repository;
    @MockitoBean JwtService jwtService;

    @Test
    void adminsCanSearchWithQueryParametersOrJson() throws Exception {
        when(service.search(any(), any())).thenReturn(Page.empty());
        for (String role : List.of("ADMIN", "SURVEY_ADMIN")) {
            mvc.perform(get("/api/questions/search").param("text", "leadership")
                            .with(user("caller").roles(role)))
                    .andExpect(status().isOk());
            mvc.perform(post("/api/questions/search").with(user("caller").roles(role))
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isOk());
        }
        verify(service, times(4)).search(any(), any());
    }

    @Test
    void anonymousSearchRequestsAreRejected() throws Exception {
        mvc.perform(get("/api/questions/search")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/questions/search")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service, excel);
    }

    @Test
    void nonAdminsCannotSearch() throws Exception {
        for (String role : List.of("USER", "OTHER")) {
            mvc.perform(get("/api/questions/search").with(user("caller").roles(role)))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/api/questions/search").with(user("caller").roles(role))
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
            mvc.perform(head("/api/questions/search").with(user("caller").roles(role)))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(service, excel);
    }

    @Test
    void searchPreflightDoesNotRequireAuthentication() throws Exception {
        mvc.perform(options("/api/questions/search")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
        verifyNoInteractions(service, excel);
    }
}
