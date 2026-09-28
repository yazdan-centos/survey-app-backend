package org.mapnaom.surveyappbackend.dto.assignment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class SaveSurveyAssignmentRequest {
    @NotNull
    private List<@NotNull UUID> userIds = new ArrayList<>();

    @NotNull
    private List<@NotNull UUID> surveyIds = new ArrayList<>();

    private Instant activeFrom;

    private Instant activeUntil;
}
