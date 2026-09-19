package org.mapnaom.surveyappbackend.service;

import lombok.RequiredArgsConstructor;
import org.mapnaom.surveyappbackend.dto.response.SaveSurveyAnswerRequest;
import org.mapnaom.surveyappbackend.entity.Question;
import org.mapnaom.surveyappbackend.entity.SurveyRole;
import org.mapnaom.surveyappbackend.repository.QuestionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


/**
 * Validates survey responses before they are persisted, ensuring each answer is unique,
 * belongs to the expected audience and survey, and contains a valid skip or level selection.
 */
@Component
@RequiredArgsConstructor
public class SurveyAnswerValidator {
    private final QuestionRepository questionRepository;

    /**
     * Validates a batch of survey answers for the supplied audience and overall survey.
     *
     * @param role the audience role expected to answer the questions
     * @param requests the submitted answers to validate
     * @param expectedSurveyId the survey id expected for all answers; if null, it is inferred from the first valid question
     * @return a map of question ids to their associated Question entities for the validated submissions
     * @throws ResponseStatusException when a submitted answer is duplicate, missing, invalid for the audience,
     *         mismatched to the survey, or contains an invalid skip/level combination
     */
    public Map<UUID, Question> validate(SurveyRole role, List<SaveSurveyAnswerRequest> requests,
                                       UUID expectedSurveyId) {
        Set<UUID> ids = new HashSet<>();
        for (SaveSurveyAnswerRequest request : requests) {
            if (!ids.add(request.getQuestionId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate question in response");
            }
        }
        Map<UUID, Question> questions = new LinkedHashMap<>();
        questionRepository.findAllById(ids).forEach(question -> questions.put(question.getId(), question));
        UUID surveyId = expectedSurveyId;
        for (SaveSurveyAnswerRequest request : requests) {
            Question question = questions.get(request.getQuestionId());
            // Ensures the submitted question exists in the database before validation continues.
            if (question == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found: " + request.getQuestionId());
            }
            // Confirms the question is intended for the audience currently submitting the survey.
            if (question.getRole() != role) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question does not belong to the response audience");
            }
            // Initializes the survey id from the first question when the caller did not provide one.
            if (surveyId == null) surveyId = question.getSurvey().getId();
            // Prevents mixed-survey submissions in a single response batch.
            if (!surveyId.equals(question.getSurvey().getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All answers must belong to the same survey");
            }
            if (Boolean.TRUE.equals(request.getSkipped())) {
                // Verifies the audience is allowed to skip this question.
                if (!role.allowsSkipping()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This audience cannot skip questions");
                }
                // Rejects skipped answers that still include a selected level value.
                if (request.getSelectedLevel() != null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A skipped answer cannot have a selected level");
                }
            } else if (request.getSelectedLevel() == null || question.getLevels().stream()
                    .noneMatch(level -> level.getLevelNumber() == request.getSelectedLevel())) {
                // Validates that a non-skipped response selects a level available for the question.
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Select an available levelNumber for question " + question.getId());
            }
        }
        return questions;
    }
}
