package org.mapnaom.surveyappbackend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapnaom.surveyappbackend.dto.user.CreateUserRequest;
import org.mapnaom.surveyappbackend.entity.User;
import org.mapnaom.surveyappbackend.entity.UserRole;
import org.mapnaom.surveyappbackend.repository.UserRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserService userService;

    @Test
    void createEncodesPasswordAndDefaultsRole() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("alice"); request.setEmail("alice@example.com"); request.setPassword("secret");
        when(passwordEncoder.encode("secret")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.create(request);

        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
        assertThat(user.getPassword()).isEqualTo("encoded");
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getEnabled()).isTrue();
        verify(passwordEncoder).encode("secret");
    }

    @Test
    void createRejectsDuplicateUsername() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("alice"); request.setEmail("alice@example.com"); request.setPassword("secret");
        when(userRepository.existsByUsername("alice")).thenReturn(true);
        assertThatThrownBy(() -> userService.create(request)).hasMessage("Username already exists");
        verify(userRepository, never()).save(any());
    }

    @Test
    void createRejectsDuplicateEmail() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("alice"); request.setEmail("alice@example.com"); request.setPassword("secret");
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);
        assertThatThrownBy(() -> userService.create(request)).hasMessage("Email already exists");
        verify(userRepository, never()).save(any());
    }
}
