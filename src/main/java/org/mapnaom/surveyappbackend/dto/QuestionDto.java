package org.mapnaom.surveyappbackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mapnaom.surveyappbackend.entity.SurveyRole;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * DTO for {@link org.mapnaom.surveyappbackend.entity.Question}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionDto implements Serializable {
    private String code;
    private String text;
    private SurveyRole role;
    private int displayOrder;
    private UUID surveyId;
    private UUID criterionId;
    private List<QuestionLevelDto> questionLevels;

}