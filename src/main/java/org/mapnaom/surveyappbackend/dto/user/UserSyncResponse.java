package org.mapnaom.surveyappbackend.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSyncResponse {
    private int totalRead;
    private int createdCount;
    private int updatedCount;
    private int skippedCount;
}
