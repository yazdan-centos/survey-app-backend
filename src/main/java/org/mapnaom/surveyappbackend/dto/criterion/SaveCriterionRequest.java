package org.mapnaom.surveyappbackend.dto.criterion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.UUID;

@Data
public class SaveCriterionRequest {
    @NotBlank
    @Size(max = 200)
    private String name;

    @NotNull
    private UUID dimensionId;
}
