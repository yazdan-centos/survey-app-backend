package org.mapnaom.surveyappbackend.controller;

import org.junit.jupiter.api.Test;
import org.mapnaom.surveyappbackend.dto.question.QuestionResponseDto;
import org.mapnaom.surveyappbackend.entity.Criterion;
import org.mapnaom.surveyappbackend.entity.Dimension;
import org.mapnaom.surveyappbackend.entity.Question;
import org.mapnaom.surveyappbackend.entity.QuestionLevel;
import org.mapnaom.surveyappbackend.entity.Survey;
import org.mapnaom.surveyappbackend.entity.SurveyRole;
import org.mapnaom.surveyappbackend.repository.QuestionRepository;
import org.mapnaom.surveyappbackend.repository.SurveyRepository;
import org.mapnaom.surveyappbackend.repository.CriterionRepository;
import org.mapnaom.surveyappbackend.service.QuestionExcelService;
import org.mapnaom.surveyappbackend.service.QuestionService;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QuestionReadControllerTest {
    @Test
    void readsQuestionTextDimensionAndLevelsWithoutSerializingEntityCycles() throws Exception {
        Survey survey = new Survey();
        survey.setId(10005L);
        Dimension dimension = new Dimension();
        dimension.setId(10006L);
        dimension.setKey("newDimension");
        dimension.setLabel("New dimension");
        dimension.setDisplayOrder(3);
        Criterion criterion = new Criterion();
        criterion.setId(10007L);
        criterion.setName("Criterion");
        criterion.setDimension(dimension);
        dimension.getCriteria().add(criterion);
        Question question = new Question();
        question.setId(10008L);
        question.setCode("Q1");
        question.setText("Question from backend");
        question.setRole(SurveyRole.BOARD);
        question.setDisplayOrder(2);
        question.setSurvey(survey);
        question.setCriterion(criterion);
        criterion.getQuestions().add(question);
        for (int number : List.of(4, 2)) {
            QuestionLevel level = new QuestionLevel();
            level.setId(10009L);
            level.setQuestion(question);
            level.setLevelNumber(number);
            level.setDescription("Description " + number);
            question.getLevels().add(level);
        }
        QuestionRepository repository = mock(QuestionRepository.class);
        when(repository.findBySurveyId(survey.getId())).thenReturn(List.of(question));
        QuestionService service = new QuestionService(repository, mock(SurveyRepository.class), mock(CriterionRepository.class));
        List<QuestionResponseDto> response = service.getSurveyQuestions(survey.getId());
        assertEquals(2, response.get(0).levels().get(0).levelNumber());
        var mvc = MockMvcBuilders.standaloneSetup(new QuestionController(service, mock(QuestionExcelService.class))).build();
        mvc.perform(get("/api/questions/survey/{id}", survey.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(question.getId().intValue()))
                .andExpect(jsonPath("$[0].surveyId").value(survey.getId().intValue()))
                .andExpect(jsonPath("$[0].text").value("Question from backend"))
                .andExpect(jsonPath("$[0].role").value("BOARD"))
                .andExpect(jsonPath("$[0].criterionId").value(criterion.getId().intValue()))
                .andExpect(jsonPath("$[0].dimension.key").value("newDimension"))
                .andExpect(jsonPath("$[0].dimension.displayOrder").value(3))
                .andExpect(jsonPath("$[0].levels[0].levelNumber").value(2))
                .andExpect(jsonPath("$[0].levels[0].description").value("Description 2"))
                .andExpect(jsonPath("$[0].levels[0].question").doesNotExist())
                .andExpect(jsonPath("$[0].survey").doesNotExist())
                .andExpect(jsonPath("$[0].dimension.criteria").doesNotExist());
    }

    @Test
    void emptySurveyReturnsAnEmptyArray() throws Exception {
        Long surveyId = 10010L;
        QuestionService service = mock(QuestionService.class);
        when(service.getSurveyQuestions(surveyId)).thenReturn(List.of());
        var mvc = MockMvcBuilders.standaloneSetup(new QuestionController(service, mock(QuestionExcelService.class))).build();
        mvc.perform(get("/api/questions/survey/{id}", surveyId))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }
}
