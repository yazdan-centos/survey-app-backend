package org.mapnaom.surveyappbackend.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.mapnaom.surveyappbackend.dto.response.SaveSurveyResponseRequest;
import org.mapnaom.surveyappbackend.dto.response.SurveyResponseDetails;
import org.mapnaom.surveyappbackend.entity.DemographicAnswer;
import org.mapnaom.surveyappbackend.entity.SurveyAnswer;
import org.mapnaom.surveyappbackend.entity.SurveyResponse;
import org.mapnaom.surveyappbackend.repository.SurveyResponseRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Validated
@RequiredArgsConstructor
public class SurveyResponseService {
    private final SurveyResponseRepository responseRepository;
    private final SurveyAnswerValidator answerValidator;

    @Transactional
    public SurveyResponseDetails create(@NotNull @Valid SaveSurveyResponseRequest request) {
        String username = currentAuthentication().getName();
        if (username == null || username.isBlank() || username.length() > 150) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid respondent username");
        }
        SurveyResponse response = new SurveyResponse();
        response.setRespondentUsername(username);
        response.setSubmittedAt(Instant.now());
        apply(response, request);
        return SurveyResponseDetails.from(persist(response));
    }

    @Transactional(readOnly = true)
    public SurveyResponseDetails findById(UUID id) {
        return SurveyResponseDetails.from(findAccessible(id, false));
    }

    @Transactional
    public SurveyResponseDetails update(UUID id, @NotNull @Valid SaveSurveyResponseRequest request) {
        SurveyResponse response = findAccessible(id, true);
        apply(response, request);
        return SurveyResponseDetails.from(persist(response));
    }

    SurveyResponse findAccessible(UUID id, boolean forUpdate) {
        Authentication authentication = currentAuthentication();
        SurveyResponse response = (forUpdate ? responseRepository.findForUpdate(id) : responseRepository.findById(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Survey response not found"));
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().equals("ROLE_SURVEY_ADMIN"));
        if (!admin && !Objects.equals(authentication.getName(), response.getRespondentUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access another respondent's submission");
        }
        return response;
    }

    SurveyResponse persist(SurveyResponse response) {
        response.setUpdatedAt(Instant.now());
        try {
            return responseRepository.saveAndFlush(response);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Survey response conflicts with existing data", exception);
        }
    }

    private void apply(SurveyResponse response, SaveSurveyResponseRequest request) {
        var questions = answerValidator.validate(request.getRole(), request.getAnswers(), null);
        var demographicKeys = new HashSet<String>();
        for (var demographic : request.getDemographics()) {
            if (!demographicKeys.add(demographic.getFieldKey())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate demographic field key");
            }
        }

        // Reuse existing children so updates keep their IDs and do not insert duplicate question pairs.
        Map<UUID, SurveyAnswer> existingAnswers = response.getAnswers().stream()
                .collect(Collectors.toMap(answer -> answer.getQuestion().getId(), Function.identity()));
        response.getAnswers().removeIf(answer -> !questions.containsKey(answer.getQuestion().getId()));
        for (var input : request.getAnswers()) {
            SurveyAnswer answer = existingAnswers.get(input.getQuestionId());
            if (answer == null) {
                answer = new SurveyAnswer();
                answer.setResponse(response);
                answer.setQuestion(questions.get(input.getQuestionId()));
                response.getAnswers().add(answer);
            }
            answer.setSelectedLevel(input.getSelectedLevel());
            answer.setSkipped(input.getSkipped());
        }

        Map<String, DemographicAnswer> existingDemographics = response.getDemographics().stream()
                .collect(Collectors.toMap(DemographicAnswer::getFieldKey, Function.identity()));
        response.getDemographics().removeIf(demographic -> !demographicKeys.contains(demographic.getFieldKey()));
        for (var input : request.getDemographics()) {
            DemographicAnswer demographic = existingDemographics.get(input.getFieldKey());
            if (demographic == null) {
                demographic = new DemographicAnswer();
                demographic.setResponse(response);
                demographic.setFieldKey(input.getFieldKey());
                response.getDemographics().add(demographic);
            }
            demographic.setValue(input.getValue());
        }
        response.setRole(request.getRole());
    }

    private Authentication currentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return authentication;
    }
}
