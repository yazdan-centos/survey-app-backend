package org.mapnaom.surveyappbackend.dto.survey;

import lombok.Builder;
import lombok.Data;
import org.mapnaom.surveyappbackend.entity.Survey;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SurveyResponseDto {
    private UUID id;
    private String title;
    private String version;
    private Boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public static SurveyResponseDto from(Survey survey) {
        return SurveyResponseDto.builder()
                .id(survey.getId())
                .title(survey.getTitle())
                .version(survey.getVersion())
                .active(survey.isActive())
                .createdAt(survey.getCreatedAt())
                .updatedAt(survey.getUpdatedAt())
                .build();
    }
}
