package org.mapnaom.surveyappbackend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapnaom.surveyappbackend.dto.survey.UpdateSurveyRequest;
import org.mapnaom.surveyappbackend.entity.Survey;
import org.mapnaom.surveyappbackend.service.SurveyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

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

        mvc.perform(put("/api/surveys/{id}", id).contentType(MediaType.APPLICATION_JSON)
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
            mvc.perform(put("/api/surveys/{id}", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(surveyService);
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(delete("/api/surveys/{id}", id))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(surveyService).delete(id);
    }

    @Test
    void endpointsReturnServiceErrors() throws Exception {
        UUID id = UUID.randomUUID();
        for (HttpStatus status : new HttpStatus[]{HttpStatus.NOT_FOUND, HttpStatus.CONFLICT}) {
            doThrow(new ResponseStatusException(status)).when(surveyService).delete(id);
            doThrow(new ResponseStatusException(status)).when(surveyService).update(eq(id), any());
            mvc.perform(delete("/api/surveys/{id}", id)).andExpect(status().is(status.value()));
            mvc.perform(put("/api/surveys/{id}", id).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Survey\",\"version\":\"2027\"}"))
                    .andExpect(status().is(status.value()));
        }
    }
}
