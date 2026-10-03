package org.mapnaom.surveyappbackend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapnaom.surveyappbackend.entity.Question;
import org.mapnaom.surveyappbackend.entity.Survey;
import org.mapnaom.surveyappbackend.entity.SurveyAnswer;
import org.mapnaom.surveyappbackend.entity.SurveyResponse;
import org.mapnaom.surveyappbackend.entity.SurveyRole;
import org.mapnaom.surveyappbackend.entity.User;
import org.mapnaom.surveyappbackend.entity.UserRole;
import org.mapnaom.surveyappbackend.repository.QuestionRepository;
import org.mapnaom.surveyappbackend.repository.SurveyRepository;
import org.mapnaom.surveyappbackend.repository.SurveyResponseRepository;
import org.mapnaom.surveyappbackend.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Seeds one complete random manager survey response for every active user. */
@Component
@Order(30)
@ConditionalOnProperty(name = "app.seed.all-user-survey-responses.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class AllUsersSurveyResponseDataInitializer implements ApplicationRunner {
    private final SurveyRepository surveyRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final SurveyResponseRepository responseRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Survey survey = surveyRepository.findByVersion("1.0.0")
                .orElseThrow(() -> new IllegalStateException("World Class survey 1.0.0 must be initialized first"));
        List<Question> questions = questionRepository.findBySurveyId(survey.getId()).stream()
                .filter(question -> question.getRole() == SurveyRole.MANAGERS)
                .toList();
        if (questions.isEmpty() || questions.stream().anyMatch(question -> question.getLevels().isEmpty())) {
            throw new IllegalStateException("World Class responses require manager questions with available levels");
        }

        List<SurveyResponse> newResponses = new ArrayList<>();
        for (User user : userRepository.findAllByDeletedFalse()) {
            if (!Boolean.TRUE.equals(user.getEnabled()) || user.getRole() != UserRole.USER
                    || responseRepository.existsByUserIdAndSurveyId(user.getId(), survey.getId())) {
                continue;
            }
            SurveyResponse response = new SurveyResponse();
            response.setRespondentUsername(user.getUsername());
            response.setUser(user);
            response.setSurvey(survey);
            response.setRole(SurveyRole.MANAGERS);
            response.setSubmittedAt(Instant.now());
            for (Question question : questions) {
                SurveyAnswer answer = new SurveyAnswer();
                answer.setResponse(response);
                answer.setQuestion(question);
                answer.setSelectedLevel(question.getLevels()
                        .get(ThreadLocalRandom.current().nextInt(question.getLevels().size()))
                        .getLevelNumber());
                answer.setSkipped(false);
                response.getAnswers().add(answer);
            }
            newResponses.add(response);
        }
        if (!newResponses.isEmpty()) {
            responseRepository.saveAllAndFlush(newResponses);
        }
        log.info("Initialized {} World Class responses for active USER accounts", newResponses.size());
    }
}
