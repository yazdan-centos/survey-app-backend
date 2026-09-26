package org.mapnaom.surveyappbackend.service;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapnaom.surveyappbackend.entity.*;
import org.mapnaom.surveyappbackend.repository.*;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionExcelServiceTest {
    @Mock SurveyRepository surveys;
    @Mock QuestionRepository questions;
    @Mock CriterionRepository criteria;
    @InjectMocks QuestionExcelService service;

    @Test
    void exportAndImportPreserveCriterionAndLevels() throws Exception {
        Survey survey = new Survey();
        survey.setId(UUID.randomUUID());
        Criterion criterion = new Criterion();
        criterion.setId(UUID.randomUUID());
        Question question = new Question();
        question.setSurvey(survey);
        question.setCriterion(criterion);
        question.setCode("Q1");
        question.setText("Leadership");
        question.setRole(SurveyRole.MANAGERS);
        QuestionLevel level = new QuestionLevel();
        level.setQuestion(question);
        level.setDescription("Excellent");
        level.setLevelNumber(4);
        question.getLevels().add(level);
        when(questions.findBySurveyId(survey.getId())).thenReturn(List.of(question));
        when(surveys.findById(survey.getId())).thenReturn(Optional.of(survey));
        when(criteria.findById(criterion.getId())).thenReturn(Optional.of(criterion));

        byte[] exported = service.exportQuestions(survey.getId());
        service.importQuestions(survey.getId(), new MockMultipartFile("file", exported));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<Question>> saved = ArgumentCaptor.forClass(Iterable.class);
        verify(questions).saveAll(saved.capture());
        assertThat(saved.getValue()).singleElement().satisfies(imported -> {
            assertThat(imported.getCriterion()).isSameAs(criterion);
            assertThat(imported.getSurvey()).isSameAs(survey);
            assertThat(imported.getText()).isEqualTo("Leadership");
            assertThat(imported.getLevels()).singleElement().satisfies(importedLevel -> {
                assertThat(importedLevel.getQuestion()).isSameAs(imported);
                assertThat(importedLevel.getDescription()).isEqualTo("Excellent");
                assertThat(importedLevel.getLevelNumber()).isEqualTo(4);
            });
        });
        try (var template = new XSSFWorkbook(new ByteArrayInputStream(service.downloadWorksheetTemplate()))) {
            assertThat(template.getSheetAt(0).getRow(0).getCell(6).getStringCellValue())
                    .isEqualTo("criterion_id");
        }
    }
}
