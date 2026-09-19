package org.mapnaom.surveyappbackend.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;
import org.mapnaom.surveyappbackend.entity.UserRole;

@Data
public class UpdateUserRequest {
    @NotBlank
    @Size(max = 100)
    private String username;
    @Size(max = 100)
    private String firstName;
    @Size(max = 100)
    private String lastName;
    @Size(max = 200)
    private String displayName;
    @Email
    @Size(max = 200)
    private String email;
    @Size(max = 100)
    private String employeeId;
    @Size(max = 100)
    private String department;
    private UserRole role;
    private Boolean enabled;
    @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank when provided")
    @Size(max = 72)
    @ToString.Exclude
    private String password;
}
