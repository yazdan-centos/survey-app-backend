package org.mapnaom.surveyappbackend.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapnaom.surveyappbackend.entity.Dimension;
import org.mapnaom.surveyappbackend.entity.Criterion;
import org.mapnaom.surveyappbackend.repository.CriterionRepository;
import org.mapnaom.surveyappbackend.entity.Question;
import org.mapnaom.surveyappbackend.entity.Survey;
import org.mapnaom.surveyappbackend.repository.DimensionRepository;
import org.mapnaom.surveyappbackend.repository.QuestionRepository;
import org.mapnaom.surveyappbackend.repository.SurveyRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataInitiatorTest {

    @Mock SurveyRepository surveyRepository;
    @Mock DimensionRepository dimensionRepository;
    @Mock CriterionRepository criterionRepository;
    @Mock QuestionRepository questionRepository;

    private DataInitiator initiator;

    @BeforeEach
    void setUp() {
        initiator = new DataInitiator(
                surveyRepository,
                dimensionRepository,
                criterionRepository,
                questionRepository,
                new ObjectMapper());
        ReflectionTestUtils.setField(initiator, "questionsResource", new ClassPathResource("surveyQuestions.json"));
    }

    @Test
    void createsCompleteSurveyDataWhenVersionIsMissing() throws Exception {
        when(surveyRepository.save(any(Survey.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(dimensionRepository.findByKey(any())).thenReturn(Optional.empty());
        when(dimensionRepository.save(any(Dimension.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(criterionRepository.save(any(Criterion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

        initiator.run(null);

        ArgumentCaptor<Question> questionCaptor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository, org.mockito.Mockito.times(100)).save(questionCaptor.capture());
        assertThat(questionCaptor.getAllValues()).hasSize(100);
        assertThat(questionCaptor.getAllValues())
                .allSatisfy(question -> assertThat(question.getLevels()).hasSize(4));
        assertThat(questionCaptor.getAllValues())
                .flatExtracting(Question::getLevels)
                .hasSize(400);
        verify(dimensionRepository, org.mockito.Mockito.times(5)).save(any(Dimension.class));
        verify(criterionRepository, org.mockito.Mockito.times(30)).save(any(Criterion.class));
    }

    @Test
    void seedsDimensionsAndCriteriaWithoutDuplicatingExistingSurvey() throws Exception {
        when(surveyRepository.existsByVersion("1.0.0")).thenReturn(true);
        when(dimensionRepository.save(any(Dimension.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(criterionRepository.save(any(Criterion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        initiator.run(null);

        verify(surveyRepository, never()).save(any());
        verify(dimensionRepository, org.mockito.Mockito.times(5)).save(any(Dimension.class));
        verify(criterionRepository, org.mockito.Mockito.times(30)).save(any(Criterion.class));
        verify(questionRepository, never()).save(any());
    }
}
