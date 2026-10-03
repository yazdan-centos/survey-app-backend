package org.mapnaom.surveyappbackend.config;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.mapnaom.surveyappbackend.entity.Question;
import org.mapnaom.surveyappbackend.entity.SurveyRole;
import org.mapnaom.surveyappbackend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.auto_quote_keyword=true",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true"
})
class SfoDataInitializerTest {
    @Autowired SurveyRepository surveys;
    @Autowired DimensionRepository dimensions;
    @Autowired CriterionRepository criteria;
    @Autowired QuestionRepository questions;
    @Autowired DemoGraphicQuestionRepository demographics;
    @Autowired EntityManager entityManager;

    @Test
    void persistsSourceAndPreservesEditsOnRestart() throws Exception {
        var initializer = new SfoDataInitializer(surveys, dimensions, criteria, questions,
                demographics, new ObjectMapper());
        initializer.run(null);
        entityManager.flush();
        entityManager.clear();
        var survey = surveys.findByVersion("sfo-1.0.0").orElseThrow();
        assertThat(survey.isActive()).isFalse();
        assertThat(dimensions.count()).isEqualTo(5);
        assertThat(criteria.count()).isEqualTo(5);
        var saved = questions.findBySurveyId(survey.getId());
        assertThat(saved).hasSize(28).allSatisfy(question -> {
            assertThat(question.getRole()).isEqualTo(SurveyRole.MANAGERS);
            assertThat(question.getLevels()).isEmpty();
            assertThat(question.getCriterion().getDimension().getKey()).startsWith("sfo-principle-");
        });
        assertThat(saved).anySatisfy(question -> assertThat(question.getText().length()).isGreaterThan(200));
        assertThat(saved).extracting(Question::getDisplayOrder)
                .containsExactlyInAnyOrderElementsOf(java.util.stream.IntStream.rangeClosed(1, 28).boxed().toList());
        var demographicQuestions = demographics.findByGroupKeyOrderByDisplayOrderAsc("sfo-managers");
        assertThat(demographicQuestions).hasSize(6);
        assertThat(demographicQuestions.get(0).getOptions())
                .containsExactly("معاون / مدیر ارشد", "مدیر میانی", "مدیر پروژه", "سایر");
        var originalIds = saved.stream().map(Question::getId).toList();
        var edited = saved.get(0);
        edited.setText("Edited prompt");
        survey.setActive(true);
        initializer.run(null);
        entityManager.flush();
        entityManager.clear();
        assertThat(surveys.count()).isEqualTo(1);
        assertThat(surveys.findById(survey.getId()).orElseThrow().isActive()).isTrue();
        assertThat(questions.findById(edited.getId()).orElseThrow().getText()).isEqualTo("Edited prompt");
        assertThat(questions.findAll()).extracting(Question::getId).containsExactlyInAnyOrderElementsOf(originalIds);
        assertThat(demographics.count()).isEqualTo(6);

        questions.deleteById(saved.get(1).getId());
        questions.flush();
        initializer.run(null);
        entityManager.flush();
        entityManager.clear();
        assertThat(questions.count()).isEqualTo(28);
        assertThat(dimensions.count()).isEqualTo(5);
        assertThat(criteria.count()).isEqualTo(5);
    }
}
