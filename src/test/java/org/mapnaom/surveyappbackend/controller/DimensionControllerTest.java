package org.mapnaom.surveyappbackend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapnaom.surveyappbackend.entity.*;
import org.mapnaom.surveyappbackend.service.DimensionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DimensionControllerTest {
    private DimensionService service;
    private MockMvc mvc;
    private Dimension entity;
    private static final String BODY = """
            {"key":"quality","label":"Quality","displayOrder":1}
            """;

    @BeforeEach
    void setUp() {
        service = mock(DimensionService.class);
        mvc = MockMvcBuilders.standaloneSetup(new DimensionController(service)).build();
        entity = new Dimension();
        entity.setId(UUID.randomUUID());
        entity.setKey("quality");
        entity.setLabel("Quality");
        Criterion child = new Criterion();
        child.setDimension(entity);
        entity.getCriteria().add(child);
    }

    @Test
    void crudReturnsDtosWithoutCircularRelationships() throws Exception {
        when(service.findAll()).thenReturn(List.of(entity));
        when(service.findById(entity.getId())).thenReturn(entity);
        when(service.create(any())).thenReturn(entity);
        when(service.update(eq(entity.getId()), any())).thenReturn(entity);
        mvc.perform(get("/api/dimensions"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(entity.getId().toString()));
        for (var request : List.of(get("/api/dimensions/{id}", entity.getId()),
                post("/api/dimensions").contentType(MediaType.APPLICATION_JSON).content(BODY),
                put("/api/dimensions/{id}", entity.getId()).contentType(MediaType.APPLICATION_JSON).content(BODY))) {
            mvc.perform(request).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(entity.getId().toString()))
                    .andExpect(jsonPath("$.key").value("quality"))
                    .andExpect(jsonPath("$.criteria").doesNotExist())
                    .andExpect(jsonPath("$.questions").doesNotExist())
                    .andExpect(jsonPath("$.dimension").doesNotExist());
        }
        mvc.perform(delete("/api/dimensions/{id}", entity.getId())).andExpect(status().isNoContent());
        verify(service).delete(entity.getId());
    }

    @Test
    void invalidRequestsAreRejectedBeforeCallingService() throws Exception {
        for (String body : List.of("{}", BODY.replace("Quality", " "), BODY.replace("Quality", "x".repeat(201)))) {
            mvc.perform(post("/api/dimensions").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
            mvc.perform(put("/api/dimensions/{id}", entity.getId()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/dimensions/invalid-uuid")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void errorsRetainTheirHttpStatus() throws Exception {
        when(service.findById(entity.getId())).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        mvc.perform(get("/api/dimensions/{id}", entity.getId())).andExpect(status().isNotFound());
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT)).when(service).delete(entity.getId());
        mvc.perform(delete("/api/dimensions/{id}", entity.getId())).andExpect(status().isConflict());
    }

}
