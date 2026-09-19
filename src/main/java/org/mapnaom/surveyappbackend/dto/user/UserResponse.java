package org.mapnaom.surveyappbackend.dto.user;

import org.mapnaom.surveyappbackend.entity.User;
import org.mapnaom.surveyappbackend.entity.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String username, String firstName, String lastName,
                           String displayName, String email, String employeeId, String department,
                           UserRole role, Boolean enabled, Boolean deleted, Boolean ldapUser,
                           String dn, Instant createdAt, Instant updatedAt) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getFirstName(), user.getLastName(),
                user.getDisplayName(), user.getEmail(), user.getEmployeeId(), user.getDepartment(),
                user.getRole(), user.getEnabled(), user.getDeleted(), user.getLdapUser(), user.getDn(),
                user.getCreatedAt(), user.getUpdatedAt());
    }
}
