package org.mapnaom.surveyappbackend.dto.question;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateQuestionLevelRequest {
    @NotBlank
    private String title;
    private Integer score;
    private Integer levelOrder;
}

