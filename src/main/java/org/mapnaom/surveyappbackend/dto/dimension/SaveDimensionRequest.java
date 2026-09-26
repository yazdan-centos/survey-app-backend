package org.mapnaom.surveyappbackend.dto.dimension;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveDimensionRequest {
    @NotBlank
    @Size(max = 80)
    private String key;

    @NotBlank
    @Size(max = 200)
    private String label;

    @NotNull
    private Integer displayOrder;
}
