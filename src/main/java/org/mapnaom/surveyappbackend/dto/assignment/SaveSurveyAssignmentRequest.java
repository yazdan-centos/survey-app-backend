package org.mapnaom.surveyappbackend.dto.assignment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
public class SaveSurveyAssignmentRequest {
    @NotNull
    private List<@NotNull Long> userIds = new ArrayList<>();

    @NotNull
    private List<@NotNull Long> surveyIds = new ArrayList<>();

    private Instant activeFrom;

    private Instant activeUntil;
}
