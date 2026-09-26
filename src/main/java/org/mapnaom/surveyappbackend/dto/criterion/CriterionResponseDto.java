package org.mapnaom.surveyappbackend.dto.criterion;

import org.mapnaom.surveyappbackend.entity.Criterion;

import java.time.Instant;
import java.util.UUID;

public record CriterionResponseDto(UUID id, String name, UUID dimensionId, Instant createdAt, Instant updatedAt) {
    public static CriterionResponseDto from(Criterion entity) {
        return new CriterionResponseDto(entity.getId(), entity.getName(), entity.getDimension().getId(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
