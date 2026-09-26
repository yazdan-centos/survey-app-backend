package org.mapnaom.surveyappbackend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapnaom.surveyappbackend.entity.*;
import org.mapnaom.surveyappbackend.service.CriterionService;
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

class CriterionControllerTest {
    private CriterionService service;
    private MockMvc mvc;
    private Criterion entity;
    private static final String BODY = """
            {"name":"Quality","dimensionId":"00000000-0000-0000-0000-000000000001"}
            """;

    @BeforeEach
    void setUp() {
        service = mock(CriterionService.class);
        mvc = MockMvcBuilders.standaloneSetup(new CriterionController(service)).build();
        entity = new Criterion();
        entity.setId(UUID.randomUUID());
        entity.setName("Quality");
        Dimension parent = new Dimension();
        parent.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        entity.setDimension(parent);
        parent.getCriteria().add(entity);
        Question child = new Question();
        child.setCriterion(entity);
        entity.getQuestions().add(child);
    }

    @Test
    void crudReturnsDtosWithoutCircularRelationships() throws Exception {
        when(service.findAll()).thenReturn(List.of(entity));
        when(service.findById(entity.getId())).thenReturn(entity);
        when(service.create(any())).thenReturn(entity);
        when(service.update(eq(entity.getId()), any())).thenReturn(entity);
        mvc.perform(get("/api/criteria"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(entity.getId().toString()));
        for (var request : List.of(get("/api/criteria/{id}", entity.getId()),
                post("/api/criteria").contentType(MediaType.APPLICATION_JSON).content(BODY),
                put("/api/criteria/{id}", entity.getId()).contentType(MediaType.APPLICATION_JSON).content(BODY))) {
            mvc.perform(request).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(entity.getId().toString()))
                    .andExpect(jsonPath("$.name").value("Quality"))
                    .andExpect(jsonPath("$.criteria").doesNotExist())
                    .andExpect(jsonPath("$.questions").doesNotExist())
                    .andExpect(jsonPath("$.dimension").doesNotExist());
        }
        mvc.perform(delete("/api/criteria/{id}", entity.getId())).andExpect(status().isNoContent());
        verify(service).delete(entity.getId());
    }

    @Test
    void invalidRequestsAreRejectedBeforeCallingService() throws Exception {
        for (String body : List.of("{}", BODY.replace("Quality", " "), BODY.replace("Quality", "x".repeat(201)))) {
            mvc.perform(post("/api/criteria").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
            mvc.perform(put("/api/criteria/{id}", entity.getId()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/criteria/invalid-uuid")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void errorsRetainTheirHttpStatus() throws Exception {
        when(service.findById(entity.getId())).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        mvc.perform(get("/api/criteria/{id}", entity.getId())).andExpect(status().isNotFound());
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT)).when(service).delete(entity.getId());
        mvc.perform(delete("/api/criteria/{id}", entity.getId())).andExpect(status().isConflict());
    }

    @Test
    void filtersCriteriaByDimension() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.findByDimension(id)).thenReturn(List.of(entity));
        mvc.perform(get("/api/criteria").param("dimensionId", id.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(entity.getId().toString()));
        verify(service).findByDimension(id);
    }

}
