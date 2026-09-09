package org.mapnaom.surveyappbackend.dto.survey;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class SurveyResponseDto {
    private UUID id;
    private String title;
    private String version;
    private Boolean active;
}
