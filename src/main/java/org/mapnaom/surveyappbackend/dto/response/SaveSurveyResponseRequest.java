package org.mapnaom.surveyappbackend.dto.response;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.mapnaom.surveyappbackend.entity.SurveyRole;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class SaveSurveyResponseRequest {
    @NotNull
    private SurveyRole role;

    /**
     * Assignment the response belongs to. When provided, the assignment must
     * belong to the current user and be currently active; it also determines
     * the survey the answers are validated against.
     */
    private UUID surveyAssignmentId;

    @NotNull
    private List<@NotNull @Valid SaveSurveyAnswerRequest> answers = new ArrayList<>();

    @NotNull
    private List<@NotNull @Valid SaveDemographicAnswerRequest> demographics = new ArrayList<>();
}
