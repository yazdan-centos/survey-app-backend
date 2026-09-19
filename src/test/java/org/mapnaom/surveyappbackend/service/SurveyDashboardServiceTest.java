package org.mapnaom.surveyappbackend.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.mapnaom.surveyappbackend.dto.survey.SurveyDashboardResponse.SurveyStats;
import org.mapnaom.surveyappbackend.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.auto_quote_keyword=true",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true"
})
@Import(SurveyDashboardService.class)
class SurveyDashboardServiceTest {
    @Autowired SurveyDashboardService service;
    @Autowired EntityManager entityManager;
    @Autowired EntityManagerFactory entityManagerFactory;

    @Test
    void emptyDatabaseReturnsZeroTotalsAndNoSurveys() {
        var dashboard = service.getDashboard();
        assertThat(dashboard.generatedAt()).isNotNull();
        assertThat(dashboard.surveys()).isEmpty();
        assertThat(dashboard.summary().totalSurveys()).isZero();
        assertThat(dashboard.summary().totalResponses()).isZero();
        assertThat(dashboard.summary().unassignedResponses()).isZero();
        assertThat(dashboard.summary().totalQuestions()).isZero();
        assertThat(dashboard.summary().totalAnswers()).isZero();
        assertThat(dashboard.summary().skipRate()).isEqualByComparingTo("0.00");
    }

    @Test
    void aggregatesWithoutMultiplyingQuestionOrResponseCounts() {
        Survey active = survey("active", true);
        Survey inactive = survey("inactive", false);
        Survey empty = survey("empty", true);
        Question first = question(active, SurveyRole.BOARD);
        Question second = question(active, SurveyRole.BOARD);
        Question manager = question(active, SurveyRole.MANAGERS);
        Question customer = question(inactive, SurveyRole.CUSTOMERS);
        response(SurveyRole.BOARD, "2026-09-15T10:00:00Z", answer(first, false), answer(second, true));
        // The same username making another submission counts as another response.
        response(SurveyRole.BOARD, "2026-09-16T10:00:00Z", answer(first, false));
        response(SurveyRole.MANAGERS, "2026-09-17T10:00:00Z", answer(manager, false));
        response(SurveyRole.CUSTOMERS, "2026-09-18T10:00:00Z", answer(customer, true));
        response(SurveyRole.SUPPLIERS, "2026-09-18T11:00:00Z");
        entityManager.flush();
        entityManager.clear();

        var dashboard = service.getDashboard();
        var summary = dashboard.summary();
        assertThat(summary.totalSurveys()).isEqualTo(3);
        assertThat(summary.activeSurveys()).isEqualTo(2);
        assertThat(summary.inactiveSurveys()).isEqualTo(1);
        assertThat(summary.totalQuestions()).isEqualTo(4);
        assertThat(summary.totalResponses()).isEqualTo(5);
        assertThat(summary.unassignedResponses()).isEqualTo(1);
        assertThat(summary.totalAnswers()).isEqualTo(5);
        assertThat(summary.answeredAnswers()).isEqualTo(3);
        assertThat(summary.skippedAnswers()).isEqualTo(2);
        assertThat(summary.skipRate()).isEqualByComparingTo("40.00");

        SurveyStats activeStats = dashboard.surveys().stream().filter(s -> s.id().equals(active.getId())).findFirst().orElseThrow();
        assertThat(activeStats.title()).isEqualTo("active");
        assertThat(activeStats.questionCount()).isEqualTo(3);
        assertThat(activeStats.responseCount()).isEqualTo(3);
        assertThat(activeStats.answerCount()).isEqualTo(4);
        assertThat(activeStats.answeredCount()).isEqualTo(3);
        assertThat(activeStats.skippedCount()).isEqualTo(1);
        assertThat(activeStats.skipRate()).isEqualByComparingTo("25.00");
        assertThat(activeStats.lastSubmittedAt()).isEqualTo(Instant.parse("2026-09-17T10:00:00Z"));
        assertThat(activeStats.audiences()).extracting(a -> a.role()).containsExactly(SurveyRole.values());
        assertThat(activeStats.audiences().stream().filter(a -> a.role() == SurveyRole.BOARD).findFirst()).get()
                .satisfies(a -> {
                    assertThat(a.responseCount()).isEqualTo(2);
                    assertThat(a.answerCount()).isEqualTo(3);
                    assertThat(a.skipRate()).isEqualByComparingTo("33.33");
                });

        SurveyStats emptyStats = dashboard.surveys().stream().filter(s -> s.id().equals(empty.getId())).findFirst().orElseThrow();
        assertThat(emptyStats.questionCount()).isZero();
        assertThat(emptyStats.responseCount()).isZero();
        assertThat(emptyStats.lastSubmittedAt()).isNull();
        assertThat(emptyStats.audiences()).hasSize(4).allSatisfy(a -> {
            assertThat(a.responseCount()).isZero();
            assertThat(a.answerCount()).isZero();
            assertThat(a.skipRate()).isEqualByComparingTo("0.00");
        });
        assertThat(dashboard.surveys()).anySatisfy(s -> {
            assertThat(s.id()).isEqualTo(inactive.getId());
            assertThat(s.active()).isFalse();
            assertThat(s.responseCount()).isEqualTo(1);
            assertThat(s.skipRate()).isEqualByComparingTo("100.00");
        });
    }

    @Test
    void questionsWithNoResponsesRemainVisible() {
        Survey survey = survey("not-started", true);
        question(survey, SurveyRole.BOARD);
        entityManager.flush();
        entityManager.clear();

        assertThat(service.getDashboard().surveys()).singleElement().satisfies(s -> {
            assertThat(s.questionCount()).isEqualTo(1);
            assertThat(s.responseCount()).isZero();
            assertThat(s.answerCount()).isZero();
            assertThat(s.skipRate()).isEqualByComparingTo("0.00");
        });
    }

    @Test
    void unattributedResponsesAreCountedEvenWithNoSurveys() {
        response(SurveyRole.BOARD, "2026-09-18T10:00:00Z");
        entityManager.flush();

        var dashboard = service.getDashboard();
        assertThat(dashboard.surveys()).isEmpty();
        assertThat(dashboard.summary().totalResponses()).isEqualTo(1);
        assertThat(dashboard.summary().unassignedResponses()).isEqualTo(1);
    }

    @Test
    void usesFourAggregateQueriesWithoutLoadingEntities() {
        for (int i = 0; i < 8; i++) {
            var survey = survey("survey-" + i, true);
            response(SurveyRole.BOARD, "2026-09-18T10:00:00Z", answer(question(survey, SurveyRole.BOARD), false));
        }
        entityManager.flush();
        entityManager.clear();
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            assertThat(service.getDashboard().surveys()).hasSize(8);
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(4);
            assertThat(statistics.getEntityLoadCount()).isZero();
            assertThat(statistics.getCollectionFetchCount()).isZero();
        } finally {
            statistics.setStatisticsEnabled(false);
        }
    }

    private Survey survey(String title, boolean active) {
        Survey survey = new Survey();
        survey.setTitle(title);
        survey.setVersion(title);
        survey.setActive(active);
        entityManager.persist(survey);
        return survey;
    }

    private Question question(Survey survey, SurveyRole role) {
        Dimension dimension = new Dimension();
        dimension.setKey(UUID.randomUUID().toString());
        dimension.setLabel("Quality");
        entityManager.persist(dimension);
        Question question = new Question();
        question.setSurvey(survey);
        question.setDimension(dimension);
        question.setCode(UUID.randomUUID().toString().substring(0, 16));
        question.setCriterion("Quality");
        question.setText("Question");
        question.setRole(role);
        entityManager.persist(question);
        return question;
    }

    private SurveyAnswer answer(Question question, boolean skipped) {
        SurveyAnswer answer = new SurveyAnswer();
        answer.setQuestion(question);
        answer.setSkipped(skipped);
        answer.setSelectedLevel(skipped ? null : 1);
        return answer;
    }

    private void response(SurveyRole role, String submittedAt, SurveyAnswer... answers) {
        SurveyResponse response = new SurveyResponse();
        response.setRole(role);
        response.setRespondentUsername("private-respondent");
        response.setSubmittedAt(Instant.parse(submittedAt));
        response.getAnswers().addAll(Arrays.asList(answers));
        response.getAnswers().forEach(answer -> answer.setResponse(response));
        entityManager.persist(response);
    }
}
