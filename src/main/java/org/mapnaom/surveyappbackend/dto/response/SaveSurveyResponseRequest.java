package org.mapnaom.surveyappbackend.dto.response;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.mapnaom.surveyappbackend.entity.SurveyRole;

import java.util.ArrayList;
import java.util.List;

@Data
public class SaveSurveyResponseRequest {
    @NotNull
    private SurveyRole role;

    @NotNull
    private List<@NotNull @Valid SaveSurveyAnswerRequest> answers = new ArrayList<>();

    @NotNull
    private List<@NotNull @Valid SaveDemographicAnswerRequest> demographics = new ArrayList<>();
}
