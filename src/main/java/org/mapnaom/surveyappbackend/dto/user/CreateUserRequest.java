package org.mapnaom.surveyappbackend.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.mapnaom.surveyappbackend.entity.UserRole;

@Data
public class CreateUserRequest {
    @NotBlank
    private String username;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    private UserRole role;
}
