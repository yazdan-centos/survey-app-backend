package org.mapnaom.surveyappbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO for {@link org.mapnaom.surveyappbackend.entity.Survey}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SurveyDto implements Serializable {
    private String title;
    private String version;
    private boolean active = true;
}