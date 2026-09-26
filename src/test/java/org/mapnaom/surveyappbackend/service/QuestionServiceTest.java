package org.mapnaom.surveyappbackend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapnaom.surveyappbackend.dto.question.CreateQuestionLevelRequest;
import org.mapnaom.surveyappbackend.dto.question.CreateQuestionRequest;
import org.mapnaom.surveyappbackend.entity.*;
import org.mapnaom.surveyappbackend.repository.QuestionRepository;
import org.mapnaom.surveyappbackend.repository.CriterionRepository;
import org.mapnaom.surveyappbackend.repository.SurveyRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {
    @Mock QuestionRepository questionRepository;
    @Mock SurveyRepository surveyRepository;
    @Mock CriterionRepository criterionRepository;
    @InjectMocks QuestionService questionService;

    @Test
    void createMapsQuestionAndLevels() {
        UUID surveyId = UUID.randomUUID();
        Survey survey = new Survey();
        Criterion criterion = new Criterion();
        criterion.setId(UUID.randomUUID());
        CreateQuestionRequest request = new CreateQuestionRequest();
        request.setSurveyId(surveyId); request.setCode("Q1"); request.setText("Leadership"); request.setRole(SurveyRole.MANAGERS);
        request.setCriterionId(criterion.getId());
        when(criterionRepository.findById(criterion.getId())).thenReturn(java.util.Optional.of(criterion));
        CreateQuestionLevelRequest level = new CreateQuestionLevelRequest();
        level.setTitle("Excellent"); level.setScore(5); level.setLevelOrder(1);
        request.setLevels(List.of(level));
        when(surveyRepository.findById(surveyId)).thenReturn(java.util.Optional.of(survey));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Question result = questionService.create(request);

        assertThat(result.getSurvey()).isSameAs(survey);
        assertThat(result.getCode()).isEqualTo("Q1");
        assertThat(result.getCriterion()).isSameAs(criterion);
        assertThat(result.getText()).isEqualTo("Leadership");
        assertThat(result.getRole()).isEqualTo(SurveyRole.MANAGERS);
        assertThat(result.getLevels()).singleElement().satisfies(l -> {
            assertThat(l.getDescription()).isEqualTo("Excellent");
            assertThat(l.getLevelNumber()).isEqualTo(5);
            assertThat(l.getQuestion()).isSameAs(result);
        });
    }

    @Test
    void createThrowsWhenCriterionMissing() {
        CreateQuestionRequest request = new CreateQuestionRequest();
        request.setSurveyId(UUID.randomUUID());
        request.setCriterionId(UUID.randomUUID());
        when(surveyRepository.findById(request.getSurveyId())).thenReturn(java.util.Optional.of(new Survey()));
        when(criterionRepository.findById(request.getCriterionId())).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> questionService.create(request)).hasMessage("Criterion not found");
        verifyNoInteractions(questionRepository);
    }

    @Test
    void createThrowsWhenSurveyMissing() {
        UUID id = UUID.randomUUID();
        CreateQuestionRequest request = new CreateQuestionRequest(); request.setSurveyId(id);
        when(surveyRepository.findById(id)).thenReturn(java.util.Optional.empty());
        assertThatThrownBy(() -> questionService.create(request)).hasMessage("Survey not found");
    }
}
