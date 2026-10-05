package org.mapnaom.surveyappbackend.dto.guide;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mapnaom.surveyappbackend.entity.GuideAudience;
import org.mapnaom.surveyappbackend.entity.GuideSeverity;
import org.mapnaom.surveyappbackend.entity.GuideStatus;
import org.mapnaom.surveyappbackend.entity.GuideTheme;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for {@link org.mapnaom.surveyappbackend.entity.Guide}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GuideDto implements Serializable {
    private GuideAudience audience;
    private GuideTheme theme = GuideTheme.INDIGO;
    private GuideSeverity severity = GuideSeverity.INFO;
    private GuideStatus status = GuideStatus.DRAFT;
    private Long surveyId;
    private String surveyVersion;
    private String title;
    private String subtitle;
    private String content;
    private String footer;
    private String startButtonLabel;
    private List<String> roleTags = new ArrayList<>();
}
