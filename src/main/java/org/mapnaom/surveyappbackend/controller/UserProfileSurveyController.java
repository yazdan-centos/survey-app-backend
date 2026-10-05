package org.mapnaom.surveyappbackend.controller;

import lombok.RequiredArgsConstructor;
import org.mapnaom.surveyappbackend.dto.assignment.ActiveSurveyDto;
import org.mapnaom.surveyappbackend.dto.assignment.ParticipatedSurveyDto;
import org.mapnaom.surveyappbackend.dto.survey.SurveyResponseDto;
import org.mapnaom.surveyappbackend.entity.User;
import org.mapnaom.surveyappbackend.repository.UserRepository;
import org.mapnaom.surveyappbackend.service.SurveyAssignmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Per-user survey endpoints: the surveys currently assigned and active for the
 * authenticated user, their participation history, and result access for
 * surveys they participated in.
 */
@CrossOrigin
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserProfileSurveyController {

    private final SurveyAssignmentService surveyAssignmentService;
    private final UserRepository userRepository;

    @GetMapping("/active-surveys")
    public ResponseEntity<List<ActiveSurveyDto>> getMyActiveSurveys() {
        User user = currentUser();
        return ResponseEntity.ok(surveyAssignmentService.findActiveSurveysForUser(user.getId()));
    }

    @GetMapping("/participated-surveys")
    public ResponseEntity<List<ParticipatedSurveyDto>> getMyParticipatedSurveys() {
        User user = currentUser();
        return ResponseEntity.ok(surveyAssignmentService.findParticipatedSurveysForUser(user.getId()));
    }

    /**
     * Survey-level result access for the authenticated user: allowed when the
     * user participated in the survey (or holds an admin role). The endpoint
     * only validates access; result payloads come from the dashboard endpoints.
     */
    @GetMapping("/surveys/{surveyId}/results")
    public ResponseEntity<Void> getMySurveyResults(@PathVariable Long surveyId) {
        User user = currentUser();
        surveyAssignmentService.assertResultAccess(user, surveyId);
        return ResponseEntity.ok().build();
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken || authentication.getName() == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Authentication is required");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found"));
    }
}
