package org.mapnaom.surveyappbackend.service;

import lombok.RequiredArgsConstructor;
import org.mapnaom.surveyappbackend.dto.assignment.ActiveSurveyDto;
import org.mapnaom.surveyappbackend.dto.assignment.ParticipatedSurveyDto;
import org.mapnaom.surveyappbackend.dto.assignment.SaveSurveyAssignmentRequest;
import org.mapnaom.surveyappbackend.dto.assignment.SurveyAssignmentResponseDto;
import org.mapnaom.surveyappbackend.dto.survey.SurveyResponseDto;
import org.mapnaom.surveyappbackend.entity.Survey;
import org.mapnaom.surveyappbackend.entity.SurveyAssignment;
import org.mapnaom.surveyappbackend.entity.SurveyAssignmentStatus;
import org.mapnaom.surveyappbackend.entity.SurveyResponse;
import org.mapnaom.surveyappbackend.entity.User;
import org.mapnaom.surveyappbackend.repository.SurveyAssignmentRepository;
import org.mapnaom.surveyappbackend.repository.SurveyRepository;
import org.mapnaom.surveyappbackend.repository.SurveyResponseRepository;
import org.mapnaom.surveyappbackend.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SurveyAssignmentService {

    private final SurveyAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final SurveyRepository surveyRepository;
    private final SurveyResponseRepository responseRepository;

    /**
     * Assigns every given survey to every given user. Existing (user, survey)
     * pairs are re-activated instead of duplicated: revocation is cleared and
     * the window is refreshed.
     */
    @Transactional
    public List<SurveyAssignmentResponseDto> assign(SaveSurveyAssignmentRequest request) {
        Instant now = Instant.now();
        List<User> users = request.getUserIds().stream().map(this::findUser).toList();
        List<Survey> surveys = request.getSurveyIds().stream().map(this::findSurvey).toList();
        List<SurveyAssignment> assignments = new ArrayList<>();
        for (User user : users) {
            for (Survey survey : surveys) {
                SurveyAssignment assignment = assignmentRepository
                        .findByUserIdAndSurveyId(user.getId(), survey.getId())
                        .orElseGet(() -> {
                            SurveyAssignment created = new SurveyAssignment();
                            created.setUser(user);
                            created.setSurvey(survey);
                            created.setAssignedAt(now);
                            return created;
                        });
                if (assignment.getAssignedAt() == null) {
                    assignment.setAssignedAt(now);
                }
                assignment.setStatus(SurveyAssignmentStatus.ASSIGNED);
                assignment.setRevokedAt(null);
                assignment.setActiveFrom(request.getActiveFrom());
                assignment.setActiveUntil(request.getActiveUntil());
                assignments.add(assignment);
            }
        }
        try {
            return assignmentRepository.saveAllAndFlush(assignments).stream()
                    .map(SurveyAssignmentResponseDto::from)
                    .toList();
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Survey assignment conflicts with existing data", exception);
        }
    }

    @Transactional(readOnly = true)
    public List<SurveyAssignmentResponseDto> findAssignmentsForSurvey(Long surveyId) {
        findSurvey(surveyId);
        return assignmentRepository.findAllBySurveyIdOrderByAssignedAtDescIdDesc(surveyId).stream()
                .map(SurveyAssignmentResponseDto::from)
                .toList();
    }

    @Transactional
    public SurveyAssignmentResponseDto revoke(Long assignmentId) {
        SurveyAssignment assignment = findAssignment(assignmentId);
        assignment.setStatus(SurveyAssignmentStatus.REVOKED);
        assignment.setRevokedAt(Instant.now());
        return SurveyAssignmentResponseDto.from(assignmentRepository.saveAndFlush(assignment));
    }

    /**
     * Marks the assignment completed once a response has been submitted.
     * Called after a successful survey response submission.
     */
    @Transactional
    public void markCompleted(User user, Survey survey) {
        assignmentRepository.findByUserIdAndSurveyId(user.getId(), survey.getId())
                .filter(assignment -> SurveyAssignmentStatus.isActiveStatus(assignment.getStatus()))
                .ifPresent(assignment -> {
                    assignment.setStatus(SurveyAssignmentStatus.COMPLETED);
                    assignment.setCompletedAt(Instant.now());
                    assignmentRepository.saveAndFlush(assignment);
                });
    }

    /**
     * Surveys listed on a user's profile: survey.active == true, not revoked,
     * inside the time window and either still open (ASSIGNED/ACTIVE) or already
     * COMPLETED by this user. Completion and the response id are user-specific.
     */
    @Transactional(readOnly = true)
    public List<ActiveSurveyDto> findActiveSurveysForUser(Long userId) {
        Instant now = Instant.now();
        Map<Long, Long> latestResponseBySurvey = new HashMap<>();
        for (SurveyResponse response : responseRepository.findAllByUserIdOrderBySubmittedAtDesc(userId)) {
            if (response.getSurvey() != null) {
                latestResponseBySurvey.putIfAbsent(response.getSurvey().getId(), response.getId());
            }
        }
        return assignmentRepository.findProfileAssignments(userId, now).stream()
                .map(assignment -> ActiveSurveyDto.from(assignment,
                        latestResponseBySurvey.get(assignment.getSurvey().getId())))
                .toList();
    }

    /**
     * Surveys a user participated in, derived from their survey responses.
     */
    @Transactional(readOnly = true)
    public List<ParticipatedSurveyDto> findParticipatedSurveysForUser(Long userId) {
        return responseRepository.findAllByUserIdOrderBySubmittedAtDesc(userId).stream()
                .map(response -> ParticipatedSurveyDto.builder()
                        .survey(SurveyResponseDto.from(response.getSurvey()))
                        .responseId(response.getId())
                        .submittedAt(response.getSubmittedAt())
                        .build())
                .toList();
    }

    /**
     * Validates that the current user has access to the results of a survey,
     * i.e. they submitted at least one response for it (admins always pass).
     */
    @Transactional(readOnly = true)
    public void assertResultAccess(User user, Long surveyId) {
        findSurvey(surveyId);
        if (isPrivileged(user)) {
            return;
        }
        if (!responseRepository.existsByUserIdAndSurveyId(user.getId(), surveyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only access results of surveys you participated in");
        }
    }

    private boolean isPrivileged(User user) {
        return user.getRole() == org.mapnaom.surveyappbackend.entity.UserRole.ADMIN
                || user.getRole() == org.mapnaom.surveyappbackend.entity.UserRole.SURVEY_ADMIN;
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id));
    }

    private Survey findSurvey(Long id) {
        return surveyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Survey not found: " + id));
    }

    private SurveyAssignment findAssignment(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Survey assignment not found: " + id));
    }
}
