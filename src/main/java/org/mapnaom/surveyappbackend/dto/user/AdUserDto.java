package org.mapnaom.surveyappbackend.dto.user;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdUserDto {
    private String username;
    private String firstName;
    private String lastName;
    private String displayName;
    private String email;
    private String employeeId;
    private String department;
    private Boolean enabled;
    private String dn;
}
