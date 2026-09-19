package org.mapnaom.surveyappbackend.security;

import org.junit.jupiter.api.Test;
import org.mapnaom.surveyappbackend.controller.UserController;
import org.mapnaom.surveyappbackend.repository.UserRepository;
import org.mapnaom.surveyappbackend.service.UserExcelService;
import org.mapnaom.surveyappbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserApiSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean UserService service;
    @MockitoBean UserExcelService excel;
    @MockitoBean UserRepository repository;
    @MockitoBean JwtService jwtService;

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mvc.perform(get("/api/users")).andExpect(status().is4xxClientError());
        verifyNoInteractions(service);
    }

    @Test
    void nonAdminsCannotAccessAnyUserManagementEndpoint() throws Exception {
        UUID id = UUID.randomUUID();
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
        UUID id = UUID.randomUUID();
        mvc.perform(delete("/api/users/{id}", id).with(user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());
        verify(service).delete(id);
    }
}
