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
import org.mapnaom.surveyappbackend.entity.User;
import org.mapnaom.surveyappbackend.repository.SurveyAssignmentRepository;
import org.mapnaom.surveyappbackend.repository.SurveyRepository;
import org.mapnaom.surveyappbackend.repository.SurveyResponseRepository;
import org.mapnaom.surveyappbackend.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @Transactional
    public SurveyAssignmentResponseDto revoke(UUID assignmentId) {
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
     * Surveys currently active for a user: survey.active == true, assignment
     * status ASSIGNED/ACTIVE, not revoked, and inside the time window.
     */
    @Transactional(readOnly = true)
    public List<ActiveSurveyDto> findActiveSurveysForUser(UUID userId) {
        Instant now = Instant.now();
        return assignmentRepository.findActiveAssignments(userId, now).stream()
                .map(ActiveSurveyDto::from)
                .toList();
    }

    /**
     * Surveys a user participated in, derived from their survey responses.
     */
    @Transactional(readOnly = true)
    public List<ParticipatedSurveyDto> findParticipatedSurveysForUser(UUID userId) {
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
    public void assertResultAccess(User user, UUID surveyId) {
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

    private User findUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id));
    }

    private Survey findSurvey(UUID id) {
        return surveyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Survey not found: " + id));
    }

    private SurveyAssignment findAssignment(UUID id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Survey assignment not found: " + id));
    }
}
