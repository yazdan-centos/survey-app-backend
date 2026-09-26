package org.mapnaom.surveyappbackend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapnaom.surveyappbackend.entity.*;
import org.mapnaom.surveyappbackend.repository.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Optional sample submissions for the manager accounts imported from the workbook. */
@Component
@Order(30)
@ConditionalOnProperty(name = "app.seed.survey-responses.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class SurveyResponseDataInitializer implements ApplicationRunner {
    private static final String SEED_KEY = "sampleDataSource";
    private static final String SEED_VALUE = "worldclass-1.0.0-five-users";
    private final SurveyRepository surveyRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final SurveyResponseRepository responseRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (responseRepository.existsByDemographic(SEED_KEY, SEED_VALUE)) {
            log.info("World Class sample responses already initialized");
            return;
        }
        Survey survey = surveyRepository.findByVersion("1.0.0")
                .orElseThrow(() -> new IllegalStateException("World Class survey 1.0.0 must be initialized first"));
        List<Question> questions = questionRepository.findBySurveyId(survey.getId()).stream()
                .filter(question -> question.getRole() == SurveyRole.MANAGERS).toList();
        if (questions.isEmpty() || questions.stream().anyMatch(question -> question.getLevels().isEmpty())) {
            throw new IllegalStateException("Sample responses require manager questions with available levels");
        }
        List<User> users = new ArrayList<>(userRepository.findAllByDeletedFalse().stream()
                .filter(user -> Boolean.TRUE.equals(user.getEnabled()) && user.getRole() == UserRole.USER)
                .toList());
        if (users.size() < 5) {
            throw new IllegalStateException("Sample responses require at least five active USER accounts");
        }
        Collections.shuffle(users);
        List<SurveyResponse> responses = new ArrayList<>();
        for (User user : users.subList(0, 5)) {
            SurveyResponse response = new SurveyResponse();
            response.setRespondentUsername(user.getUsername());
            response.setRole(SurveyRole.MANAGERS);
            response.setSubmittedAt(Instant.now());
            for (Question question : questions) {
                SurveyAnswer answer = new SurveyAnswer();
                answer.setResponse(response);
                answer.setQuestion(question);
                answer.setSelectedLevel(pick(question.getLevels()).getLevelNumber());
                answer.setSkipped(false);
                response.getAnswers().add(answer);
            }
            demographic(response, "department", user.getDepartment() == null || user.getDepartment().isBlank()
                    ? pick(List.of("Engineering", "Operations", "Finance", "Human Resources")) : user.getDepartment());
            demographic(response, "gender", pick(List.of("female", "male", "preferNotToSay")));
            demographic(response, "ageRange", pick(List.of("35-44", "45-54", "55-64")));
            demographic(response, "education", pick(List.of("bachelor", "master", "doctorate")));
            demographic(response, "yearsOfExperience", pick(List.of("5-9", "10-14", "15-19")));
            demographic(response, SEED_KEY, SEED_VALUE);
            responses.add(response);
        }
        responseRepository.saveAllAndFlush(responses);
        log.info("Initialized five World Class sample responses with {} answers each", questions.size());
    }

    private static <T> T pick(List<T> values) {
        return values.get(ThreadLocalRandom.current().nextInt(values.size()));
    }

    private static void demographic(SurveyResponse response, String key, String value) {
        DemographicAnswer answer = new DemographicAnswer();
        answer.setResponse(response);
        answer.setFieldKey(key);
        answer.setValue(value);
        response.getDemographics().add(answer);
    }
}
