package org.mapnaom.surveyappbackend.config;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapnaom.surveyappbackend.entity.Criterion;
import org.mapnaom.surveyappbackend.entity.Survey;
import org.mapnaom.surveyappbackend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.auto_quote_keyword=true",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true"
})
class DataInitiatorPersistenceTest {
    @Autowired SurveyRepository surveys;
    @Autowired DimensionRepository dimensions;
    @Autowired CriterionRepository criteria;
    @Autowired QuestionRepository questions;
    @Autowired EntityManager entityManager;
    private DataInitiator initializer;

    @BeforeEach
    void setUp() {
        initializer = new DataInitiator(surveys, dimensions, criteria, questions, new ObjectMapper());
        ReflectionTestUtils.setField(initializer, "questionsResource", new ClassPathResource("surveyQuestions.json"));
    }

    @Test
    void savesCriteriaAndReusesThemOnRestart() throws Exception {
        initializer.run(null);
        entityManager.flush();
        entityManager.clear();
        var originalIds = criteria.findAll().stream().map(Criterion::getId).toList();
        assertThat(originalIds).hasSize(30);
        assertThat(dimensions.count()).isEqualTo(5);
        assertThat(questions.count()).isEqualTo(100);
        assertThat(criteria.findAll()).allSatisfy(criterion -> {
            assertThat(criterion.getName()).isNotBlank();
            assertThat(criterion.getDimension().getId()).isNotNull();
            assertThat(criterion.getQuestions()).isNotEmpty();
        });

        initializer.run(null);
        entityManager.flush();
        entityManager.clear();
        assertThat(criteria.findAll()).extracting(Criterion::getId)
                .containsExactlyInAnyOrderElementsOf(originalIds);
        assertThat(dimensions.count()).isEqualTo(5);
        assertThat(surveys.count()).isEqualTo(1);
        assertThat(questions.count()).isEqualTo(100);
    }

    @Test
    void savesMissingCriteriaWhenSurveyAlreadyExists() throws Exception {
        Survey survey = new Survey();
        survey.setTitle("Existing survey");
        survey.setVersion("1.0.0");
        surveys.saveAndFlush(survey);

        initializer.run(null);
        entityManager.flush();
        entityManager.clear();
        assertThat(criteria.count()).isEqualTo(30);
        assertThat(dimensions.count()).isEqualTo(5);
        assertThat(surveys.count()).isEqualTo(1);
        assertThat(surveys.findById(survey.getId()).orElseThrow().getTitle()).isEqualTo("Existing survey");
        assertThat(questions.count()).isZero();

        // Simulate a partially seeded database while retaining the existing survey.
        Criterion missing = criteria.findAll().get(0);
        var dimensionId = missing.getDimension().getId();
        String name = missing.getName();
        criteria.delete(missing);
        criteria.flush();
        entityManager.clear();
        initializer.run(null);
        entityManager.flush();
        entityManager.clear();
        assertThat(criteria.count()).isEqualTo(30);
        assertThat(criteria.findByDimensionIdAndName(dimensionId, name)).isPresent();
        assertThat(questions.count()).isZero();
    }

    @Test
    void reusesCriteriaThatExistBeforeSurveyCreation() throws Exception {
        initializer.run(null);
        entityManager.flush();
        entityManager.clear();
        var originalIds = criteria.findAll().stream().map(Criterion::getId).toList();
        questions.deleteAll();
        questions.flush();
        surveys.deleteAll();
        surveys.flush();
        entityManager.clear();

        initializer.run(null);
        entityManager.flush();
        entityManager.clear();
        assertThat(criteria.findAll()).extracting(Criterion::getId)
                .containsExactlyInAnyOrderElementsOf(originalIds);
        assertThat(questions.findAll()).allSatisfy(question ->
                assertThat(question.getCriterion().getId()).isIn(originalIds));
        assertThat(questions.count()).isEqualTo(100);
    }
}
