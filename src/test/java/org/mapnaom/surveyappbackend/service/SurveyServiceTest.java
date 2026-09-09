package org.mapnaom.surveyappbackend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapnaom.surveyappbackend.dto.survey.CreateSurveyRequest;
import org.mapnaom.surveyappbackend.entity.Survey;
import org.mapnaom.surveyappbackend.repository.SurveyRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

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
        UUID id = UUID.randomUUID();
        when(surveyRepository.findById(id)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> surveyService.findById(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Survey not found");
    }

    @Test
    void findAllDelegatesToRepository() {
        List<Survey> surveys = List.of(new Survey());
        when(surveyRepository.findAll()).thenReturn(surveys);
        assertThat(surveyService.findAll()).isSameAs(surveys);
    }
}
