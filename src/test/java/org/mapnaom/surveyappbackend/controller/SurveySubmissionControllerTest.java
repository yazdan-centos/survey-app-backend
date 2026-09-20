package org.mapnaom.surveyappbackend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapnaom.surveyappbackend.dto.response.*;
import org.mapnaom.surveyappbackend.entity.SurveyRole;
import org.mapnaom.surveyappbackend.exception.SurveySubmissionExceptionHandler;
import org.mapnaom.surveyappbackend.service.SurveyAnswerService;
import org.mapnaom.surveyappbackend.service.SurveyResponseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SurveySubmissionControllerTest {
    private SurveyResponseService responses;
    private SurveyAnswerService answers;
    private MockMvc mvc;
    private final UUID responseId = UUID.randomUUID();
    private final UUID answerId = UUID.randomUUID();
    private final UUID questionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        responses = mock(SurveyResponseService.class);
        answers = mock(SurveyAnswerService.class);
        mvc = MockMvcBuilders.standaloneSetup(new SurveyResponseController(responses), new SurveyAnswerController(answers))
                .setControllerAdvice(new SurveySubmissionExceptionHandler()).build();
    }

    @Test
    void createsResponseWithLocationAndFlatAnswerDetails() throws Exception {
        when(responses.create(any())).thenReturn(response());
        mvc.perform(post("/api/survey-responses").contentType(MediaType.APPLICATION_JSON).content(responseBody()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/survey-responses/" + responseId))
                .andExpect(jsonPath("$.id").value(responseId.toString()))
                .andExpect(jsonPath("$.answers[0].questionId").value(questionId.toString()))
                .andExpect(jsonPath("$.answers[0].response").doesNotExist());
        verify(responses).create(argThat(r -> r.getRole() == SurveyRole.BOARD && r.getAnswers().size() == 1
                && r.getAnswers().get(0).getQuestionId().equals(questionId)));
    }

    @Test
    void getsAndUpdatesResponses() throws Exception {
        when(responses.findById(responseId)).thenReturn(response());
        when(responses.update(eq(responseId), any())).thenReturn(response());
        mvc.perform(get("/api/survey-responses/{id}", responseId)).andExpect(status().isOk());
        mvc.perform(put("/api/survey-responses/{id}", responseId)
                        .contentType(MediaType.APPLICATION_JSON).content(responseBody()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.respondentUsername").value("alice"));
        verify(responses).update(eq(responseId), any());
    }

    @Test
    void createsAndUpdatesIndividualAnswers() throws Exception {
        when(answers.create(eq(responseId), any())).thenReturn(answer());
        when(answers.update(eq(responseId), eq(answerId), any())).thenReturn(answer());
        mvc.perform(post("/api/survey-responses/{id}/answers", responseId)
                        .contentType(MediaType.APPLICATION_JSON).content(answerBody()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/survey-responses/" + responseId + "/answers/" + answerId));
        mvc.perform(put("/api/survey-responses/{id}/answers/{answerId}", responseId, answerId)
                        .contentType(MediaType.APPLICATION_JSON).content(answerBody()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.selectedLevel").value(1));
        verify(answers).update(eq(responseId), eq(answerId), argThat(r -> r.getQuestionId().equals(questionId)));
    }

    @Test
    void readsAnswerListAndSingleAnswer() throws Exception {
        when(answers.findByResponseId(responseId)).thenReturn(List.of(answer()));
        when(answers.findById(responseId, answerId)).thenReturn(answer());
        mvc.perform(get("/api/survey-responses/{id}/answers", responseId)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(answerId.toString()));
        mvc.perform(get("/api/survey-responses/{id}/answers/{answerId}", responseId, answerId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.responseId").value(responseId.toString()));
    }

    @Test
    void rejectsMissingRoleAndInvalidNestedRequestsBeforeCallingService() throws Exception {
        for (String body : List.of("{}", "{\"role\":\"BOARD\",\"answers\":[{}]}",
                "{\"role\":\"BOARD\",\"answers\":[null]}",
                "{\"role\":\"BOARD\",\"demographics\":[{\"fieldKey\":\" \",\"value\":\"\"}]}",
                "{\"role\":\"BOARD\",\"answers\":null}")) {
            mvc.perform(post("/api/survey-responses").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").exists());
        }
        mvc.perform(post("/api/survey-responses/{id}/answers", responseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":\"" + questionId + "\",\"selectedLevel\":0}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(responses, answers);
    }

    @Test
    void preservesErrorStatusesAndDetails() throws Exception {
        for (HttpStatus status : List.of(HttpStatus.NOT_FOUND, HttpStatus.FORBIDDEN, HttpStatus.CONFLICT, HttpStatus.BAD_REQUEST)) {
            doThrow(new ResponseStatusException(status, "Submission error")).when(responses).update(eq(responseId), any());
            mvc.perform(put("/api/survey-responses/{id}", responseId)
                            .contentType(MediaType.APPLICATION_JSON).content(responseBody()))
                    .andExpect(status().is(status.value())).andExpect(jsonPath("$.detail").value("Submission error"));
        }
    }

    private String answerBody() {
        return "{\"questionId\":\"" + questionId + "\",\"selectedLevel\":1,\"skipped\":false}";
    }

    private String responseBody() {
        return "{\"role\":\"BOARD\",\"answers\":[" + answerBody() + "],\"demographics\":[]}";
    }

    private SurveyAnswerDetails answer() {
        return new SurveyAnswerDetails(answerId, responseId, questionId, 1, false, Instant.now(), Instant.now());
    }

    private SurveyResponseDetails response() {
        return new SurveyResponseDetails(responseId, SurveyRole.BOARD, "alice", Instant.now(),
                List.of(answer()), List.of(), Instant.now(), Instant.now());
    }
}
