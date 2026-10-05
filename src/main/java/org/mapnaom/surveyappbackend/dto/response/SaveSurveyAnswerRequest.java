package org.mapnaom.surveyappbackend.dto.response;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;


@Data
public class SaveSurveyAnswerRequest {
    @NotNull
    private Long questionId;

    @Positive
    private Integer selectedLevel;

    @NotNull
    private Boolean skipped = false;
}
