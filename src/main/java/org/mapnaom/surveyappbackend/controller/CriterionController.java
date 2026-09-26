package org.mapnaom.surveyappbackend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mapnaom.surveyappbackend.dto.criterion.SaveCriterionRequest;
import org.mapnaom.surveyappbackend.dto.criterion.CriterionResponseDto;
import org.mapnaom.surveyappbackend.service.CriterionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin
@RestController
@RequestMapping("/api/criteria")
@RequiredArgsConstructor
public class CriterionController {
    private final CriterionService service;

    @GetMapping
    public ResponseEntity<List<CriterionResponseDto>> getAllCriteria(@RequestParam(required = false) UUID dimensionId) {
        return ResponseEntity.ok((dimensionId == null ? service.findAll() : service.findByDimension(dimensionId))
                .stream().map(CriterionResponseDto::from).toList());
    }

    @GetMapping("/{criterionId}")
    public ResponseEntity<CriterionResponseDto> getCriterionById(@PathVariable UUID criterionId) {
        return ResponseEntity.ok(CriterionResponseDto.from(service.findById(criterionId)));
    }

    @PostMapping
    public ResponseEntity<CriterionResponseDto> createCriterion(@Valid @RequestBody SaveCriterionRequest request) {
        return ResponseEntity.ok(CriterionResponseDto.from(service.create(request)));
    }

    @PutMapping("/{criterionId}")
    public ResponseEntity<CriterionResponseDto> updateCriterion(@PathVariable UUID criterionId,
            @Valid @RequestBody SaveCriterionRequest request) {
        return ResponseEntity.ok(CriterionResponseDto.from(service.update(criterionId, request)));
    }

    @DeleteMapping("/{criterionId}")
    public ResponseEntity<Void> deleteCriterion(@PathVariable UUID criterionId) {
        service.delete(criterionId);
        return ResponseEntity.noContent().build();
    }
}
