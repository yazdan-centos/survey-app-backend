package org.mapnaom.surveyappbackend.service;

import lombok.RequiredArgsConstructor;
import org.mapnaom.surveyappbackend.dto.dimension.SaveDimensionRequest;
import org.mapnaom.surveyappbackend.entity.Dimension;
import org.mapnaom.surveyappbackend.repository.DimensionRepository;
import org.mapnaom.surveyappbackend.repository.CriterionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DimensionService {
    private final DimensionRepository repository;
    private final CriterionRepository criterionRepository;

    @Transactional(readOnly = true)
    public List<Dimension> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Dimension findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dimension not found"));
    }

    @Transactional
    public Dimension create(SaveDimensionRequest request) {
        return save(new Dimension(), request);
    }

    @Transactional
    public Dimension update(UUID id, SaveDimensionRequest request) {
        return save(findById(id), request);
    }

    private Dimension save(Dimension entity, SaveDimensionRequest request) {
        entity.setKey(request.getKey());
        entity.setLabel(request.getLabel());
        entity.setDisplayOrder(request.getDisplayOrder());
        try {
            return repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Dimension key must be unique", exception);
        }
    }

    @Transactional
    public void delete(UUID id) {
        Dimension entity = findById(id);
        if (criterionRepository.existsByDimensionId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Dimension cannot be deleted while related data exists");
        }
        try {
            repository.delete(entity);
            repository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Dimension cannot be deleted while related data exists", exception);
        }
    }
}
