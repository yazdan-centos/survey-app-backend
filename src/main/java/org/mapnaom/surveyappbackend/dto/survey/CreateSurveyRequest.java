package org.mapnaom.surveyappbackend.dto.survey;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateSurveyRequest {
    @NotBlank
    private String title;
    private String version;
    private Boolean active;
}
