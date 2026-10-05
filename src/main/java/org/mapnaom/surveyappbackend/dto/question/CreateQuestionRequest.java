package org.mapnaom.surveyappbackend.dto.question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import org.mapnaom.surveyappbackend.entity.SurveyRole;

import java.util.List;

@Data
public class CreateQuestionRequest {
    @NotNull
    private Long surveyId;

    @NotNull
    @JsonAlias("criterion_id")
    private Long criterionId;

    @NotBlank
    private String code;

    @NotBlank
    private String text;

    @NotNull
    private SurveyRole role;

    private List<CreateQuestionLevelRequest> levels;
}

