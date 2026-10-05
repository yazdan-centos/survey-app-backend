package org.mapnaom.surveyappbackend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapnaom.surveyappbackend.dto.survey.CreateSurveyRequest;
import org.mapnaom.surveyappbackend.dto.survey.UpdateSurveyRequest;
import org.mapnaom.surveyappbackend.entity.Survey;
import org.mapnaom.surveyappbackend.repository.SurveyRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SurveyServiceTest {
    @Mock SurveyRepository surveyRepository;
    @InjectMocks SurveyService surveyService;

    @Test
    void createUsesDefaultsAndPersistsSurvey() {
        CreateSurveyRequest request = new CreateSurveyRequest();
        request.setTitle("Annual survey");
        request.setVersion("2026");
        when(surveyRepository.save(any(Survey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Survey result = surveyService.create(request);

        assertThat(result.getTitle()).isEqualTo("Annual survey");
        assertThat(result.getVersion()).isEqualTo("2026");
        assertThat(result.isActive()).isTrue();
        verify(surveyRepository).save(result);
    }

    @Test
    void findByIdThrowsWhenSurveyDoesNotExist() {
        Long id = 10049L;
        when(surveyRepository.findById(id)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> surveyService.findById(id))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void missingActiveSurveyReturnsNotFound() {
        when(surveyRepository.findByActiveTrue()).thenReturn(Optional.empty());
        assertThatThrownBy(() -> surveyService.findActiveSurvey())
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void updateChangesFieldsAndPreservesIdentityAndQuestions() {
        Long id = 10050L;
        Survey survey = new Survey();
        survey.setId(id);
        var questions = survey.getQuestions();
        when(surveyRepository.findById(id)).thenReturn(Optional.of(survey));
        when(surveyRepository.saveAndFlush(survey)).thenReturn(survey);
        UpdateSurveyRequest request = updateRequest();
        request.setActive(false);

        Survey result = surveyService.update(id, request);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getTitle()).isEqualTo("Updated survey");
        assertThat(result.getVersion()).isEqualTo("2027");
        assertThat(result.isActive()).isFalse();
        assertThat(result.getQuestions()).isSameAs(questions);
        verify(surveyRepository).saveAndFlush(survey);
    }

    @Test
    void updatePreservesActiveWhenOmitted() {
        Long id = 10051L;
        Survey survey = new Survey();
        survey.setActive(false);
        when(surveyRepository.findById(id)).thenReturn(Optional.of(survey));
        when(surveyRepository.saveAndFlush(survey)).thenReturn(survey);

        assertThat(surveyService.update(id, updateRequest()).isActive()).isFalse();
    }

    @Test
    void updateMissingSurveyReturnsNotFoundWithoutSaving() {
        assertThatThrownBy(() -> surveyService.update(10052L, updateRequest()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(surveyRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateDuplicateVersionReturnsConflict() {
        Long id = 10053L;
        Survey survey = new Survey();
        when(surveyRepository.findById(id)).thenReturn(Optional.of(survey));
        when(surveyRepository.saveAndFlush(survey))
                .thenThrow(new DataIntegrityViolationException("duplicate version"));

        assertThatThrownBy(() -> surveyService.update(id, updateRequest()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void deleteRemovesExistingSurvey() {
        Long id = 10054L;
        Survey survey = new Survey();
        when(surveyRepository.findById(id)).thenReturn(Optional.of(survey));

        surveyService.delete(id);

        verify(surveyRepository).delete(survey);
        verify(surveyRepository).flush();
    }

    @Test
    void deleteMissingSurveyReturnsNotFoundWithoutDeleting() {
        assertThatThrownBy(() -> surveyService.delete(10055L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(surveyRepository, never()).delete(any(Survey.class));
    }

    @Test
    void deleteWithRelatedDataReturnsConflict() {
        Long id = 10056L;
        when(surveyRepository.findById(id)).thenReturn(Optional.of(new Survey()));
        doThrow(new DataIntegrityViolationException("linked questions")).when(surveyRepository).flush();

        assertThatThrownBy(() -> surveyService.delete(id))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    private UpdateSurveyRequest updateRequest() {
        UpdateSurveyRequest request = new UpdateSurveyRequest();
        request.setTitle("Updated survey");
        request.setVersion("2027");
        return request;
    }

    @Test
    void findAllDelegatesToRepository() {
        List<Survey> surveys = List.of(new Survey());
        when(surveyRepository.findAll()).thenReturn(surveys);
        assertThat(surveyService.findAll()).isSameAs(surveys);
    }
}
