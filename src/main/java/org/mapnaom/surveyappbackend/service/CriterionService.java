package org.mapnaom.surveyappbackend.service;

import lombok.RequiredArgsConstructor;
import org.mapnaom.surveyappbackend.dto.criterion.SaveCriterionRequest;
import org.mapnaom.surveyappbackend.entity.Criterion;
import org.mapnaom.surveyappbackend.repository.CriterionRepository;
import org.mapnaom.surveyappbackend.repository.QuestionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CriterionService {
    private final CriterionRepository repository;
    private final DimensionService dimensionService;
    private final QuestionRepository questionRepository;

    @Transactional(readOnly = true)
    public List<Criterion> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Criterion findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Criterion not found"));
    }

    @Transactional(readOnly = true)
    public List<Criterion> findByDimension(UUID dimensionId) {
        dimensionService.findById(dimensionId);
        return repository.findByDimensionId(dimensionId);
    }

    @Transactional
    public Criterion create(SaveCriterionRequest request) {
        return save(new Criterion(), request);
    }

    @Transactional
    public Criterion update(UUID id, SaveCriterionRequest request) {
        return save(findById(id), request);
    }

    private Criterion save(Criterion entity, SaveCriterionRequest request) {
        entity.setName(request.getName());
        entity.setDimension(dimensionService.findById(request.getDimensionId()));
        try {
            return repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Criterion name must be unique within its dimension", exception);
        }
    }

    @Transactional
    public void delete(UUID id) {
        Criterion entity = findById(id);
        if (questionRepository.existsByCriterionId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Criterion cannot be deleted while related data exists");
        }
        try {
            repository.delete(entity);
            repository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Criterion cannot be deleted while related data exists", exception);
        }
    }
}
