package org.mapnaom.surveyappbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * DTO for {@link org.mapnaom.surveyappbackend.entity.QuestionLevel}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionLevelDto implements Serializable {
    private int levelNumber;
    private String description;
    private UUID questionId;
    private double title;
    private double score;
}