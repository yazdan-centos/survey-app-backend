package org.mapnaom.surveyappbackend.dto.user;

import lombok.Data;
import org.mapnaom.surveyappbackend.entity.UserRole;

@Data
public class UserSearchRequest {
    private String q;
    private String username;
    private String email;
    private String department;
    private String employeeId;
    private UserRole role;
    private Boolean enabled;
    private Boolean ldapUser;
}
