package org.mapnaom.surveyappbackend.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveDemographicAnswerRequest {
    @NotBlank
    @Size(max = 100)
    private String fieldKey;

    @NotBlank
    private String value;
}
