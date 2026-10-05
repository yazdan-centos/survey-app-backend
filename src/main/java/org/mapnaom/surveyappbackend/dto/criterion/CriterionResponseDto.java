package org.mapnaom.surveyappbackend.dto.criterion;

import org.mapnaom.surveyappbackend.entity.Criterion;

import java.time.Instant;

public record CriterionResponseDto(Long id, String name, Long dimensionId, Instant createdAt, Instant updatedAt) {
    public static CriterionResponseDto from(Criterion entity) {
        return new CriterionResponseDto(entity.getId(), entity.getName(), entity.getDimension().getId(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
