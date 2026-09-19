package org.mapnaom.surveyappbackend.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.mapnaom.surveyappbackend.dto.response.SaveSurveyAnswerRequest;
import org.mapnaom.surveyappbackend.dto.response.SurveyAnswerDetails;
import org.mapnaom.surveyappbackend.entity.SurveyAnswer;
import org.mapnaom.surveyappbackend.repository.SurveyAnswerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Validated
@RequiredArgsConstructor
public class SurveyAnswerService {
    private final SurveyAnswerRepository answerRepository;
    private final SurveyResponseService responseService;
    private final SurveyAnswerValidator answerValidator;

    @Transactional
    public SurveyAnswerDetails create(UUID responseId, @NotNull @Valid SaveSurveyAnswerRequest request) {
        var response = responseService.findAccessible(responseId, true);
        if (response.getAnswers().stream().anyMatch(answer -> answer.getQuestion().getId().equals(request.getQuestionId()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An answer already exists for this question");
        }
        UUID surveyId = response.getAnswers().isEmpty() ? null
                : response.getAnswers().get(0).getQuestion().getSurvey().getId();
        var questions = answerValidator.validate(response.getRole(), List.of(request), surveyId);
        SurveyAnswer answer = new SurveyAnswer();
        answer.setResponse(response);
        answer.setQuestion(questions.get(request.getQuestionId()));
        answer.setSelectedLevel(request.getSelectedLevel());
        answer.setSkipped(request.getSkipped());
        response.getAnswers().add(answer);
        var saved = responseService.persist(response);
        // Cascading merge may replace a new child with a managed copy containing its generated ID.
        return saved.getAnswers().stream()
                .filter(persisted -> persisted.getQuestion().getId().equals(request.getQuestionId()))
                .findFirst().map(SurveyAnswerDetails::from).orElseThrow();
    }

    @Transactional
    public SurveyAnswerDetails update(UUID responseId, UUID answerId, @NotNull @Valid SaveSurveyAnswerRequest request) {
        var response = responseService.findAccessible(responseId, true);
        SurveyAnswer answer = findAnswer(responseId, answerId);
        if (!answer.getQuestion().getId().equals(request.getQuestionId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An existing answer's question cannot be changed");
        }
        answerValidator.validate(response.getRole(), List.of(request), answer.getQuestion().getSurvey().getId());
        answer.setSelectedLevel(request.getSelectedLevel());
        answer.setSkipped(request.getSkipped());
        responseService.persist(response);
        return SurveyAnswerDetails.from(answer);
    }

    @Transactional(readOnly = true)
    public List<SurveyAnswerDetails> findByResponseId(UUID responseId) {
        return responseService.findAccessible(responseId, false).getAnswers().stream()
                .map(SurveyAnswerDetails::from).toList();
    }

    @Transactional(readOnly = true)
    public SurveyAnswerDetails findById(UUID responseId, UUID answerId) {
        responseService.findAccessible(responseId, false);
        return SurveyAnswerDetails.from(findAnswer(responseId, answerId));
    }

    private SurveyAnswer findAnswer(UUID responseId, UUID answerId) {
        return answerRepository.findByIdAndResponseId(answerId, responseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Survey answer not found in this response"));
    }
}
