package org.mapnaom.surveyappbackend.dto.dimension;

import org.mapnaom.surveyappbackend.entity.Dimension;

import java.time.Instant;
import java.util.UUID;

public record DimensionResponseDto(UUID id, String key, String label, int displayOrder, Instant createdAt, Instant updatedAt) {
    public static DimensionResponseDto from(Dimension entity) {
        return new DimensionResponseDto(entity.getId(), entity.getKey(), entity.getLabel(), entity.getDisplayOrder(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
