package org.mapnaom.surveyappbackend.dto.question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.mapnaom.surveyappbackend.entity.SurveyRole;

import java.util.List;
import java.util.UUID;

@Data
public class CreateQuestionRequest {
    @NotNull
    private UUID surveyId;

    @NotBlank
    private String code;

    @NotBlank
    private String text;

    @NotNull
    private SurveyRole role;

    private List<CreateQuestionLevelRequest> levels;
}

