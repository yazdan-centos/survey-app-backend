package org.mapnaom.surveyappbackend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mapnaom.surveyappbackend.dto.dimension.SaveDimensionRequest;
import org.mapnaom.surveyappbackend.dto.dimension.DimensionResponseDto;
import org.mapnaom.surveyappbackend.service.DimensionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/dimensions")
@RequiredArgsConstructor
public class DimensionController {
    private final DimensionService service;

    @GetMapping
    public ResponseEntity<List<DimensionResponseDto>> getAllDimensions() {
        return ResponseEntity.ok(service.findAll()
                .stream().map(DimensionResponseDto::from).toList());
    }

    @GetMapping("/{dimensionId}")
    public ResponseEntity<DimensionResponseDto> getDimensionById(@PathVariable Long dimensionId) {
        return ResponseEntity.ok(DimensionResponseDto.from(service.findById(dimensionId)));
    }

    @PostMapping
    public ResponseEntity<DimensionResponseDto> createDimension(@Valid @RequestBody SaveDimensionRequest request) {
        return ResponseEntity.ok(DimensionResponseDto.from(service.create(request)));
    }

    @PutMapping("/{dimensionId}")
    public ResponseEntity<DimensionResponseDto> updateDimension(@PathVariable Long dimensionId,
            @Valid @RequestBody SaveDimensionRequest request) {
        return ResponseEntity.ok(DimensionResponseDto.from(service.update(dimensionId, request)));
    }

    @DeleteMapping("/{dimensionId}")
    public ResponseEntity<Void> deleteDimension(@PathVariable Long dimensionId) {
        service.delete(dimensionId);
        return ResponseEntity.noContent().build();
    }
}
