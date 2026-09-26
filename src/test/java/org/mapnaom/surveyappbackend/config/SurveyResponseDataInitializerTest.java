package org.mapnaom.surveyappbackend.config;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapnaom.surveyappbackend.entity.*;
import org.mapnaom.surveyappbackend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.auto_quote_keyword=true",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true"
})
class SurveyResponseDataInitializerTest {
    @Autowired SurveyRepository surveys;
    @Autowired DimensionRepository dimensions;
    @Autowired QuestionRepository questions;
    @Autowired UserRepository users;
    @Autowired SurveyResponseRepository responses;
    @Autowired EntityManager entityManager;
    private SurveyResponseDataInitializer initializer;

    @BeforeEach
    void setUp() throws Exception {
        DataInitiator surveySeed = new DataInitiator(surveys, dimensions, questions, new ObjectMapper());
        ReflectionTestUtils.setField(surveySeed, "questionsResource", new ClassPathResource("surveyQuestions.json"));
        surveySeed.run(null);
        initializer = new SurveyResponseDataInitializer(surveys, questions, users, responses);
    }

    @Test
    void persistsFiveCompleteSubmissionsAndDoesNotRepeatOnRestart() {
        for (int i = 0; i < 8; i++) user("manager" + i, true, false, UserRole.USER);
        user("disabled", false, false, UserRole.USER);
        user("deleted", true, true, UserRole.USER);
        user("admin", true, false, UserRole.ADMIN);
        initializer.run(null);
        entityManager.clear();

        var saved = responses.findAll();
        var managerQuestions = questions.findBySurveyId(surveys.findByVersion("1.0.0").orElseThrow().getId())
                .stream().filter(q -> q.getRole() == SurveyRole.MANAGERS).map(Question::getId).toList();
        assertThat(saved).hasSize(5);
        assertThat(saved).extracting(SurveyResponse::getRespondentUsername).doesNotHaveDuplicates()
                .allSatisfy(username -> assertThat(username).startsWith("manager"));
        for (var response : saved) {
            assertThat(response.getRole()).isEqualTo(SurveyRole.MANAGERS);
            assertThat(response.getSubmittedAt()).isNotNull();
            assertThat(response.getAnswers()).extracting(a -> a.getQuestion().getId())
                    .containsExactlyInAnyOrderElementsOf(managerQuestions);
            assertThat(response.getAnswers()).allSatisfy(answer -> {
                assertThat(answer.isSkipped()).isFalse();
                assertThat(answer.getQuestion().getLevels()).extracting(QuestionLevel::getLevelNumber)
                        .contains(answer.getSelectedLevel());
                assertThat(answer.getResponse().getId()).isEqualTo(response.getId());
            });
            assertThat(response.getDemographics()).extracting(DemographicAnswer::getFieldKey)
                    .containsExactlyInAnyOrder("department", "gender", "ageRange", "education",
                            "yearsOfExperience", "sampleDataSource");
            assertThat(response.getDemographics()).allSatisfy(d -> assertThat(d.getValue()).isNotBlank());
        }
        initializer.run(null);
        entityManager.clear();
        assertThat(responses.findAll()).extracting(SurveyResponse::getId)
                .containsExactlyInAnyOrderElementsOf(saved.stream().map(SurveyResponse::getId).toList());
    }

    @Test
    void insufficientEligibleUsersLeavesNoPartialSubmissions() {
        for (int i = 0; i < 4; i++) user("manager" + i, true, false, UserRole.USER);
        user("disabled", false, false, UserRole.USER);
        assertThatIllegalStateException().isThrownBy(() -> initializer.run(null))
                .withMessageContaining("at least five");
        assertThat(responses.count()).isZero();
    }

    @Test
    void missingLevelsLeavesNoPartialSubmissions() {
        var question = questions.findAll().stream().filter(q -> q.getRole() == SurveyRole.MANAGERS)
                .findFirst().orElseThrow();
        question.getLevels().clear();
        questions.flush();
        assertThatIllegalStateException().isThrownBy(() -> initializer.run(null))
                .withMessageContaining("available levels");
        assertThat(responses.count()).isZero();
    }

    private void user(String username, boolean enabled, boolean deleted, UserRole role) {
        users.save(User.builder().username(username).enabled(enabled).deleted(deleted)
                .ldapUser(false).role(role).department("Engineering").build());
    }
}
