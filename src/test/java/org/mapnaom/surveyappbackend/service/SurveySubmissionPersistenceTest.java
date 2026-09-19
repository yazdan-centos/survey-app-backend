package org.mapnaom.surveyappbackend.service;

import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapnaom.surveyappbackend.dto.response.*;
import org.mapnaom.surveyappbackend.entity.*;
import org.mapnaom.surveyappbackend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.auto_quote_keyword=true",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true"
})
@Import({SurveyResponseService.class, SurveyAnswerService.class, SurveyAnswerValidator.class,
        ValidationAutoConfiguration.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@WithMockUser(username = "alice")
class SurveySubmissionPersistenceTest {
    @Autowired SurveyResponseService responses;
    @Autowired SurveyAnswerService answers;
    @Autowired SurveyResponseRepository responseRepository;
    @Autowired SurveyAnswerRepository answerRepository;
    @Autowired SurveyRepository surveyRepository;
    @Autowired QuestionRepository questionRepository;
    @Autowired DimensionRepository dimensionRepository;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactionManager;

    private Question first;
    private Question second;
    private Question third;
    private Question managerQuestion;
    private Question otherSurveyQuestion;

    @BeforeEach
    void createQuestions() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Survey survey = survey("main");
            Dimension dimension = new Dimension();
            dimension.setKey("quality");
            dimension.setLabel("Quality");
            dimensionRepository.saveAndFlush(dimension);
            first = question(survey, dimension, "Q1", SurveyRole.BOARD);
            second = question(survey, dimension, "Q2", SurveyRole.BOARD);
            third = question(survey, dimension, "Q3", SurveyRole.BOARD);
            managerQuestion = question(survey, dimension, "M1", SurveyRole.MANAGERS);
            otherSurveyQuestion = question(survey("other"), dimension, "Q1", SurveyRole.BOARD);
        });
    }

    @AfterEach
    void cleanDatabase() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            responseRepository.deleteAll();
            responseRepository.flush();
            questionRepository.deleteAll();
            questionRepository.flush();
            surveyRepository.deleteAll();
            dimensionRepository.deleteAll();
        });
    }

    @Test
    void createsResponseAndCascadesAnswersAndDemographics() {
        var request = request(answer(first, 1), answer(second, 2));
        request.setDemographics(List.of(demographic("department", "Engineering")));

        var saved = responses.create(request);
        var loaded = responses.findById(saved.id());

        assertThat(loaded.respondentUsername()).isEqualTo("alice");
        assertThat(loaded.submittedAt()).isNotNull();
        assertThat(loaded.createdAt()).isNotNull();
        assertThat(loaded.answers()).hasSize(2).allSatisfy(a -> {
            assertThat(a.id()).isNotNull();
            assertThat(a.responseId()).isEqualTo(saved.id());
        });
        assertThat(loaded.demographics()).singleElement().satisfies(d -> {
            assertThat(d.id()).isNotNull();
            assertThat(d.value()).isEqualTo("Engineering");
        });
        assertThat(answerRepository.count()).isEqualTo(2);
    }

    @Test
    void replacementUpdatesExistingChildrenAndDeletesOrphans() {
        var original = request(answer(first, 1), answer(second, 1));
        original.setDemographics(List.of(demographic("department", "Old"), demographic("region", "West")));
        var saved = responses.findById(responses.create(original).id());
        var firstAnswer = saved.answers().stream().filter(a -> a.questionId().equals(first.getId())).findFirst().orElseThrow();
        var removedAnswer = saved.answers().stream().filter(a -> a.questionId().equals(second.getId())).findFirst().orElseThrow();
        var department = saved.demographics().stream().filter(d -> d.fieldKey().equals("department")).findFirst().orElseThrow();
        var update = request(answer(first, 2), answer(third, 1));
        update.setDemographics(List.of(demographic("department", "New")));

        responses.update(saved.id(), update);
        var loaded = responses.findById(saved.id());

        assertThat(loaded.respondentUsername()).isEqualTo(saved.respondentUsername());
        assertThat(loaded.submittedAt()).isEqualTo(saved.submittedAt());
        assertThat(loaded.createdAt()).isEqualTo(saved.createdAt());
        assertThat(loaded.answers()).hasSize(2);
        assertThat(loaded.answers().stream().filter(a -> a.questionId().equals(first.getId())).findFirst()).get()
                .satisfies(a -> {
                    assertThat(a.id()).isEqualTo(firstAnswer.id());
                    assertThat(a.selectedLevel()).isEqualTo(2);
                });
        assertThat(answerRepository.existsById(removedAnswer.id())).isFalse();
        assertThat(loaded.demographics()).singleElement().satisfies(d -> {
            assertThat(d.id()).isEqualTo(department.id());
            assertThat(d.value()).isEqualTo("New");
        });
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                assertThat(entityManager.createQuery("select count(d) from DemographicAnswer d", Long.class).getSingleResult())
                        .isEqualTo(1));
    }

    @Test
    void createsAndUpdatesIndividualAnswerWithoutChangingSiblings() {
        var response = responses.create(request(answer(first, 1)));
        var created = answers.create(response.id(), answer(second, 1));
        assertThat(created.id()).isNotNull();
        var saved = answers.findById(response.id(), created.id());
        var skip = answer(second, null);
        skip.setSkipped(true);

        answers.update(response.id(), saved.id(), skip);
        var skipped = answers.findById(response.id(), saved.id());
        assertThat(skipped.skipped()).isTrue();
        assertThat(skipped.selectedLevel()).isNull();

        var updated = answers.update(response.id(), saved.id(), answer(second, 2));
        assertThat(updated.id()).isEqualTo(saved.id());
        assertThat(updated.createdAt()).isEqualTo(saved.createdAt());
        assertThat(updated.selectedLevel()).isEqualTo(2);
        assertThat(updated.skipped()).isFalse();
        assertThat(answers.findByResponseId(response.id())).hasSize(2);
        assertThat(answers.findById(response.id(), response.answers().get(0).id()).selectedLevel()).isEqualTo(1);
    }

    @Test
    void emptyResponseCanReceiveAnswersAndReplacementCanRemoveAllChildren() {
        var response = responses.create(request());
        answers.create(response.id(), answer(first, 1));
        assertThat(answers.findByResponseId(response.id())).hasSize(1);
        responses.update(response.id(), request());
        assertThat(answers.findByResponseId(response.id())).isEmpty();
        assertThat(answerRepository.count()).isZero();
    }

    @Test
    void duplicateIndividualAnswerReturns409WithoutAddingRows() {
        var response = responses.create(request(answer(first, 1)));
        assertStatus(() -> answers.create(response.id(), answer(first, 2)), HttpStatus.CONFLICT);
        assertThat(answerRepository.count()).isEqualTo(1);
    }

    @Test
    void invalidReplacementRollsBackAndKeepsOriginalAnswersAndDemographics() {
        var original = request(answer(first, 1), answer(second, 1));
        original.setDemographics(List.of(demographic("department", "Original")));
        var response = responses.create(original);
        var update = request(answer(first, 2), answer(second, 999));
        update.setDemographics(List.of(demographic("department", "Changed")));

        assertStatus(() -> responses.update(response.id(), update), HttpStatus.BAD_REQUEST);

        var loaded = responses.findById(response.id());
        assertThat(loaded.answers()).allSatisfy(a -> assertThat(a.selectedLevel()).isEqualTo(1));
        assertThat(loaded.demographics().get(0).value()).isEqualTo("Original");
        assertThat(loaded.role()).isEqualTo(SurveyRole.BOARD);
    }

    @Test
    void invalidIndividualUpdateKeepsOriginalAnswer() {
        var response = responses.create(request(answer(first, 1)));
        UUID answerId = response.answers().get(0).id();
        assertStatus(() -> answers.update(response.id(), answerId, answer(first, 99)), HttpStatus.BAD_REQUEST);
        assertThat(answers.findById(response.id(), answerId).selectedLevel()).isEqualTo(1);
    }

    @Test
    void mixedSurveysCannotBePersistedOrAddedToAnExistingResponse() {
        assertStatus(() -> responses.create(request(answer(first, 1), answer(otherSurveyQuestion, 1))), HttpStatus.BAD_REQUEST);
        assertThat(responseRepository.count()).isZero();
        var response = responses.create(request(answer(first, 1)));
        assertStatus(() -> answers.create(response.id(), answer(otherSurveyQuestion, 1)), HttpStatus.BAD_REQUEST);
        assertThat(answerRepository.count()).isEqualTo(1);
    }

    @Test
    void duplicateQuestionsAndDemographicKeysAreRejected() {
        assertStatus(() -> responses.create(request(answer(first, 1), answer(first, 2))), HttpStatus.BAD_REQUEST);
        var request = request(answer(first, 1));
        request.setDemographics(List.of(demographic("region", "East"), demographic("region", "West")));
        assertStatus(() -> responses.create(request), HttpStatus.BAD_REQUEST);
        assertThat(responseRepository.count()).isZero();
    }

    @Test
    void audienceMustMatchQuestionAndManagersCannotSkip() {
        assertStatus(() -> responses.create(request(answer(managerQuestion, 1))), HttpStatus.BAD_REQUEST);
        var skip = answer(managerQuestion, null);
        skip.setSkipped(true);
        var request = request(skip);
        request.setRole(SurveyRole.MANAGERS);
        assertStatus(() -> responses.create(request), HttpStatus.BAD_REQUEST);
    }

    @Test
    void skippedAnswersCannotSpecifyLevelAndNonSkippedAnswersRequireValidLevel() {
        var skip = answer(first, 1);
        skip.setSkipped(true);
        assertStatus(() -> responses.create(request(skip)), HttpStatus.BAD_REQUEST);
        assertStatus(() -> responses.create(request(answer(first, null))), HttpStatus.BAD_REQUEST);
        assertStatus(() -> responses.create(request(answer(first, 999))), HttpStatus.BAD_REQUEST);
    }

    @Test
    void unknownQuestionResponseAndAnswerReturn404() {
        var input = answer(first, 1);
        input.setQuestionId(UUID.randomUUID());
        assertStatus(() -> responses.create(request(input)), HttpStatus.NOT_FOUND);
        assertStatus(() -> responses.findById(UUID.randomUUID()), HttpStatus.NOT_FOUND);
        assertStatus(() -> responses.update(UUID.randomUUID(), request()), HttpStatus.NOT_FOUND);
        assertStatus(() -> answers.create(UUID.randomUUID(), answer(first, 1)), HttpStatus.NOT_FOUND);
        var response = responses.create(request());
        assertStatus(() -> answers.findById(response.id(), UUID.randomUUID()), HttpStatus.NOT_FOUND);
    }

    @Test
    void answerCannotBeMovedToAnotherQuestionOrUpdatedThroughAnotherResponse() {
        var firstResponse = responses.create(request(answer(first, 1)));
        var secondResponse = responses.create(request());
        UUID id = firstResponse.answers().get(0).id();
        assertStatus(() -> answers.update(firstResponse.id(), id, answer(second, 1)), HttpStatus.BAD_REQUEST);
        assertStatus(() -> answers.update(secondResponse.id(), id, answer(first, 1)), HttpStatus.NOT_FOUND);
        assertStatus(() -> answers.findById(secondResponse.id(), id), HttpStatus.NOT_FOUND);
        assertThat(answers.findById(firstResponse.id(), id).questionId()).isEqualTo(first.getId());
    }

    @Test
    void otherRespondentsCannotReadOrModifySubmissionsOrAnswers() {
        var response = responses.create(request(answer(first, 1)));
        UUID answerId = response.answers().get(0).id();
        authenticate("bob", "USER");
        assertStatus(() -> responses.findById(response.id()), HttpStatus.FORBIDDEN);
        assertStatus(() -> responses.update(response.id(), request()), HttpStatus.FORBIDDEN);
        assertStatus(() -> answers.findById(response.id(), answerId), HttpStatus.FORBIDDEN);
        assertStatus(() -> answers.findByResponseId(response.id()), HttpStatus.FORBIDDEN);
        assertStatus(() -> answers.create(response.id(), answer(second, 1)), HttpStatus.FORBIDDEN);
        assertStatus(() -> answers.update(response.id(), answerId, answer(first, 2)), HttpStatus.FORBIDDEN);
    }

    @Test
    void administratorsCanEditAnotherRespondentsSubmissionWithoutChangingOwner() {
        var response = responses.create(request(answer(first, 1)));
        authenticate("reviewer", "SURVEY_ADMIN");
        responses.update(response.id(), request(answer(first, 2)));
        authenticate("admin", "ADMIN");
        var loaded = responses.findById(response.id());
        assertThat(loaded.respondentUsername()).isEqualTo("alice");
        assertThat(loaded.answers().get(0).selectedLevel()).isEqualTo(2);
    }

    @Test
    void unauthenticatedCallerCannotCreateSubmissions() {
        SecurityContextHolder.clearContext();
        assertStatus(() -> responses.create(request()), HttpStatus.UNAUTHORIZED);
    }

    @Test
    void directServiceCallsValidateNestedRequests() {
        var request = request(answer(first, 0));
        assertThatThrownBy(() -> responses.create(request)).isInstanceOf(ConstraintViolationException.class);
        request.setAnswers(null);
        assertThatThrownBy(() -> responses.create(request)).isInstanceOf(ConstraintViolationException.class);
        assertThat(responseRepository.count()).isZero();
    }

    private Survey survey(String version) {
        Survey survey = new Survey();
        survey.setTitle(version);
        survey.setVersion(version);
        return surveyRepository.saveAndFlush(survey);
    }

    private Question question(Survey survey, Dimension dimension, String code, SurveyRole role) {
        Question question = new Question();
        question.setSurvey(survey);
        question.setDimension(dimension);
        question.setCode(code);
        question.setCriterion("Quality");
        question.setText("How effective is the service?");
        question.setRole(role);
        for (int number = 1; number <= 2; number++) {
            QuestionLevel level = new QuestionLevel();
            level.setQuestion(question);
            level.setLevelNumber(number);
            level.setDescription("Level " + number);
            question.getLevels().add(level);
        }
        return questionRepository.saveAndFlush(question);
    }

    private SaveSurveyResponseRequest request(SaveSurveyAnswerRequest... answers) {
        SaveSurveyResponseRequest request = new SaveSurveyResponseRequest();
        request.setRole(SurveyRole.BOARD);
        request.setAnswers(List.of(answers));
        return request;
    }

    private SaveSurveyAnswerRequest answer(Question question, Integer level) {
        SaveSurveyAnswerRequest request = new SaveSurveyAnswerRequest();
        request.setQuestionId(question.getId());
        request.setSelectedLevel(level);
        return request;
    }

    private SaveDemographicAnswerRequest demographic(String key, String value) {
        SaveDemographicAnswerRequest request = new SaveDemographicAnswerRequest();
        request.setFieldKey(key);
        request.setValue(value);
        return request;
    }

    private void authenticate(String name, String role) {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                name, "unused", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private void assertStatus(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, HttpStatus status) {
        assertThatThrownBy(action).isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode()).isEqualTo(status));
    }
}
