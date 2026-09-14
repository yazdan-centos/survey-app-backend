package org.mapnaom.surveyappbackend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapnaom.surveyappbackend.entity.User;
import org.mapnaom.surveyappbackend.entity.UserRole;
import org.mapnaom.surveyappbackend.repository.UserRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserInitializerTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;

    private AdminUserInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new AdminUserInitializer(userRepository, passwordEncoder);
        ReflectionTestUtils.setField(initializer, "username", "admin");
        ReflectionTestUtils.setField(initializer, "password", "secret");
    }

    @Test
    void createsAdminWhenMissing() {
        when(passwordEncoder.encode("secret")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        initializer.run(null);

        var userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User user = userCaptor.getValue();
        assertThat(user.getUsername()).isEqualTo("admin");
        assertThat(user.getPassword()).isEqualTo("encoded");
        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(user.getEnabled()).isTrue();
        assertThat(user.getDeleted()).isFalse();
        assertThat(user.getLdapUser()).isFalse();
    }

    @Test
    void leavesExistingAdminUnchanged() {
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        initializer.run(null);

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }
}
