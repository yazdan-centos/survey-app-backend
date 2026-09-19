package org.mapnaom.surveyappbackend.service;

import lombok.RequiredArgsConstructor;
import org.mapnaom.surveyappbackend.dto.survey.CreateSurveyRequest;
import org.mapnaom.surveyappbackend.dto.survey.UpdateSurveyRequest;
import org.mapnaom.surveyappbackend.entity.Survey;
import org.mapnaom.surveyappbackend.repository.SurveyRepository;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SurveyService {

    private final SurveyRepository surveyRepository;

    public Survey create(CreateSurveyRequest request) {
        Survey survey = new Survey();
        survey.setTitle(request.getTitle());
        survey.setVersion(request.getVersion());
        survey.setActive(request.getActive() != null ? request.getActive() : true);
        return surveyRepository.save(survey);
    }

    public List<Survey> findAll() {
        return surveyRepository.findAll();
    }

    public Survey findById(UUID id) {
        return surveyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Survey not found"));
    }

    @Transactional
    public Survey update(UUID id, UpdateSurveyRequest request) {
        Survey survey = findById(id);
        survey.setTitle(request.getTitle());
        survey.setVersion(request.getVersion());
        if (request.getActive() != null) {
            survey.setActive(request.getActive());
        }
        try {
            return surveyRepository.saveAndFlush(survey);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Survey update conflicts with existing data; version must be unique", exception);
        }
    }

    @Transactional
    public void delete(UUID id) {
        Survey survey = findById(id);
        try {
            surveyRepository.delete(survey);
            surveyRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Survey cannot be deleted while related data exists", exception);
        }
    }

    public Survey findActiveSurvey() {
        return surveyRepository.findByActiveTrue()
                .orElseThrow(() -> new RuntimeException("No active survey found"));
    }
}
