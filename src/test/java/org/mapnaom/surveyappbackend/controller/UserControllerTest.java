package org.mapnaom.surveyappbackend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapnaom.surveyappbackend.dto.user.UserImportResponse;
import org.mapnaom.surveyappbackend.entity.User;
import org.mapnaom.surveyappbackend.entity.UserRole;
import org.mapnaom.surveyappbackend.service.UserExcelService;
import org.mapnaom.surveyappbackend.service.UserService;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerTest {
    private UserService service;
    private UserExcelService excel;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(UserService.class);
        excel = mock(UserExcelService.class);
        mvc = MockMvcBuilders.standaloneSetup(new UserController(service, excel))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver()).build();
    }

    @Test
    void findAllAndFindByIdNeverExposePasswords() throws Exception {
        User user = user();
        when(service.findAll()).thenReturn(List.of(user));
        when(service.findById(user.getId())).thenReturn(user);
        mvc.perform(get("/api/users")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
        mvc.perform(get("/api/users/{id}", user.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void createAndUpdateNeverExposePasswords() throws Exception {
        User user = user();
        when(service.create(any())).thenReturn(user);
        when(service.update(eq(user.getId()), any())).thenReturn(user);
        String body = "{\"username\":\"alice\",\"email\":\"alice@example.com\",\"password\":\"secret\"}";
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(put("/api/users/{id}", user.getId()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.password").doesNotExist());
        verify(service).update(eq(user.getId()), argThat(r -> r.getUsername().equals("alice") && r.getPassword().equals("secret")));
    }

    @Test
    void searchBindsFiltersPageAndSort() throws Exception {
        when(service.search(any(), any())).thenReturn(new PageImpl<>(List.of(user()), PageRequest.of(1, 5), 6));
        mvc.perform(get("/api/users/search").param("q", "alice").param("role", "USER")
                        .param("department", "IT").param("enabled", "false").param("ldapUser", "true")
                        .param("page", "1").param("size", "5").param("sort", "email,desc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.content[0].password").doesNotExist());
        verify(service).search(argThat(f -> f.getQ().equals("alice") && f.getRole() == UserRole.USER
                        && f.getDepartment().equals("IT") && !f.getEnabled() && f.getLdapUser()),
                argThat(p -> p.getPageNumber() == 1 && p.getPageSize() == 5
                        && p.getSort().getOrderFor("email").isDescending()));
    }

    @Test
    void invalidInputReturns400() throws Exception {
        mvc.perform(put("/api/users/{id}", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\" \",\"email\":\"invalid\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/users/not-a-uuid")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/users/search").param("role", "INVALID")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void deleteReturns204AndErrorsIncludeReason() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(delete("/api/users/{id}", id)).andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(service).delete(id);
        when(service.findById(id)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        mvc.perform(get("/api/users/{id}", id)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("User not found"));
        when(service.update(eq(id), any())).thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists"));
        mvc.perform(put("/api/users/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("Email already exists"));
    }

    @Test
    void importBindsMultipartAndReturnsCount() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "users.xlsx", "application/octet-stream", new byte[]{1});
        when(excel.importByExcel(any())).thenReturn(new UserImportResponse(3));
        mvc.perform(multipart("/api/users/import").file(file)).andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(3));
        verify(excel).importByExcel(file);
        mvc.perform(multipart("/api/users/import")).andExpect(status().isBadRequest());
    }

    @Test
    void templateHasDownloadHeadersAndWorkbookBytes() throws Exception {
        byte[] bytes = {1, 2, 3};
        when(excel.downloadWorksheetTemplate()).thenReturn(bytes);
        mvc.perform(get("/api/users/template")).andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=users-template.xlsx"))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(content().bytes(bytes));
    }

    private User user() {
        User user = User.builder().username("alice").password("secret-hash").role(UserRole.USER)
                .enabled(true).deleted(false).ldapUser(false).build();
        user.setId(UUID.randomUUID());
        return user;
    }
}
