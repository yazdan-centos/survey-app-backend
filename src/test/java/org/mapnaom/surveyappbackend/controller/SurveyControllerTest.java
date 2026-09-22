package org.mapnaom.surveyappbackend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapnaom.surveyappbackend.dto.survey.UpdateSurveyRequest;
import org.mapnaom.surveyappbackend.entity.Survey;
import org.mapnaom.surveyappbackend.entity.Question;
import org.mapnaom.surveyappbackend.service.SurveyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.List;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SurveyControllerTest {
    private SurveyService surveyService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        surveyService = mock(SurveyService.class);
        mvc = MockMvcBuilders.standaloneSetup(new SurveyController(surveyService)).build();
    }

    @Test
    void updateReturnsUpdatedSurvey() throws Exception {
        UUID id = UUID.randomUUID();
        Survey survey = new Survey();
        survey.setId(id);
        survey.setTitle("Updated survey");
        survey.setVersion("2027");
        survey.setActive(false);
        when(surveyService.update(eq(id), any(UpdateSurveyRequest.class))).thenReturn(survey);

        mvc.perform(put("/api/v1/surveys/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Updated survey","version":"2027","active":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.title").value("Updated survey"))
                .andExpect(jsonPath("$.active").value(false));
        verify(surveyService).update(eq(id), argThat(request ->
                request.getTitle().equals("Updated survey") && request.getVersion().equals("2027")
                        && Boolean.FALSE.equals(request.getActive())));
    }

    @Test
    void updateRejectsInvalidFields() throws Exception {
        for (String body : new String[]{"{}", "{\"title\":\" \",\"version\":\"2027\"}",
                "{\"title\":\"Survey\",\"version\":\" \"}",
                "{\"title\":\"" + "x".repeat(201) + "\",\"version\":\"2027\"}",
                "{\"title\":\"Survey\",\"version\":\"" + "x".repeat(51) + "\"}"}) {
            mvc.perform(put("/api/v1/surveys/{id}", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(surveyService);
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(delete("/api/v1/surveys/{id}", id))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(surveyService).delete(id);
    }

    @Test
    void endpointsReturnServiceErrors() throws Exception {
        UUID id = UUID.randomUUID();
        for (HttpStatus status : new HttpStatus[]{HttpStatus.NOT_FOUND, HttpStatus.CONFLICT}) {
            doThrow(new ResponseStatusException(status)).when(surveyService).delete(id);
            doThrow(new ResponseStatusException(status)).when(surveyService).update(eq(id), any());
            mvc.perform(delete("/api/v1/surveys/{id}", id)).andExpect(status().is(status.value()));
            mvc.perform(put("/api/v1/surveys/{id}", id).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Survey\",\"version\":\"2027\"}"))
                    .andExpect(status().is(status.value()));
        }
    }

    @Test
    void surveyEndpointsReturnCompleteJsonWithoutCircularEntityRelationships() throws Exception {
        Survey survey = new Survey();
        survey.setId(UUID.randomUUID());
        survey.setTitle("Survey with questions");
        survey.setVersion("2026");
        Question question = new Question();
        question.setSurvey(survey);
        survey.getQuestions().add(question);
        when(surveyService.findAll()).thenReturn(List.of(survey));
        when(surveyService.findActiveSurvey()).thenReturn(survey);
        when(surveyService.create(any())).thenReturn(survey);
        when(surveyService.update(eq(survey.getId()), any())).thenReturn(survey);

        var requests = List.of(
                get("/api/v1/surveys"),
                get("/api/v1/surveys/active"),
                post("/api/v1/surveys").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Survey\",\"version\":\"2026\",\"active\":true}"),
                put("/api/v1/surveys/{id}", survey.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Survey\",\"version\":\"2026\",\"active\":true}"));
        var mapper = JsonMapper.builder().build();
        for (var request : requests) {
            String body = mvc.perform(request).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            var json = mapper.readTree(body);
            var item = json.isArray() ? json.get(0) : json;
            assertEquals(survey.getId().toString(), item.get("id").asText());
            assertEquals("2026", item.get("version").asText());
            assertTrue(item.get("active").asBoolean());
            assertFalse(item.has("questions"));
            assertFalse(body.contains("hibernateLazyInitializer"));
        }
    }
}
