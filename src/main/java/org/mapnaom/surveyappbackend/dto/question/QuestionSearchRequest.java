package org.mapnaom.surveyappbackend.dto.question;

import lombok.Data;
import org.mapnaom.surveyappbackend.entity.SurveyRole;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.UUID;

@Data
public class QuestionSearchRequest {
    private String q;
    private UUID id;
    private String code;
    private String text;
    private SurveyRole role;
    private Integer displayOrder;
    private UUID surveyId;
    private String surveyTitle;
    private String surveyVersion;
    private Boolean surveyActive;
    private UUID criterionId;
    private String criterionName;
    private UUID dimensionId;
    private String dimensionKey;
    private String dimensionLabel;
    private Integer dimensionDisplayOrder;
    private UUID levelId;
    private Integer levelNumber;
    /** Matches QuestionLevel.description; question wording is filtered with text. */
    private String description;
    private Double levelTitle;
    private Double levelScore;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant createdAtFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant createdAtTo;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant updatedAtFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant updatedAtTo;
}
