package org.mapnaom.surveyappbackend.dto.response;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.UUID;

@Data
public class SaveSurveyAnswerRequest {
    @NotNull
    private UUID questionId;

    @Positive
    private Integer selectedLevel;

    @NotNull
    private Boolean skipped = false;
}
