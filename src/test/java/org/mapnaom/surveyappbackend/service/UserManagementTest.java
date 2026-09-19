package org.mapnaom.surveyappbackend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapnaom.surveyappbackend.dto.user.UpdateUserRequest;
import org.mapnaom.surveyappbackend.dto.user.UserSearchRequest;
import org.mapnaom.surveyappbackend.entity.User;
import org.mapnaom.surveyappbackend.entity.UserRole;
import org.mapnaom.surveyappbackend.repository.UserRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserManagementTest {
    @Mock UserRepository repository;
    @Mock PasswordEncoder encoder;
    @InjectMocks UserService service;

    @Test
    void readsUseNonDeletedUsers() {
        User user = user();
        when(repository.findAllByDeletedFalse()).thenReturn(List.of(user));
        when(repository.findByIdAndDeletedFalse(user.getId())).thenReturn(Optional.of(user));
        assertThat(service.findAll()).containsExactly(user);
        assertThat(service.findById(user.getId())).isSameAs(user);
    }

    @Test
    void missingUserReturns404ForReadUpdateAndDelete() {
        UUID id = UUID.randomUUID();
        assertStatus(() -> service.findById(id), HttpStatus.NOT_FOUND);
        assertStatus(() -> service.update(id, request()), HttpStatus.NOT_FOUND);
        assertStatus(() -> service.delete(id), HttpStatus.NOT_FOUND);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void updateEncodesPasswordAndChangesProfile() {
        User user = existingUser();
        UpdateUserRequest request = request();
        request.setPassword("new-secret");
        request.setEnabled(false);
        request.setRole(UserRole.SURVEY_ADMIN);
        when(encoder.encode("new-secret")).thenReturn("new-hash");
        when(repository.saveAndFlush(user)).thenReturn(user);

        User result = service.update(user.getId(), request);

        assertThat(result.getUsername()).isEqualTo("updated");
        assertThat(result.getEmail()).isEqualTo("updated@example.com");
        assertThat(result.getFirstName()).isEqualTo("Updated");
        assertThat(result.getDepartment()).isEqualTo("IT");
        assertThat(result.getRole()).isEqualTo(UserRole.SURVEY_ADMIN);
        assertThat(result.getEnabled()).isFalse();
        assertThat(result.getPassword()).isEqualTo("new-hash");
        assertThat(result.getDeleted()).isFalse();
        assertThat(result.getLdapUser()).isFalse();
    }

    @Test
    void omittedPasswordRoleAndEnabledArePreserved() {
        User user = existingUser();
        when(repository.saveAndFlush(user)).thenReturn(user);
        User result = service.update(user.getId(), request());
        assertThat(result.getPassword()).isEqualTo("old-hash");
        assertThat(result.getRole()).isEqualTo(UserRole.USER);
        assertThat(result.getEnabled()).isTrue();
        verifyNoInteractions(encoder);
    }

    @Test
    void duplicateUsernameAndEmailReturn409() {
        User user = existingUser();
        when(repository.existsByUsernameAndIdNot("updated", user.getId())).thenReturn(true);
        assertStatus(() -> service.update(user.getId(), request()), HttpStatus.CONFLICT);
        when(repository.existsByUsernameAndIdNot("updated", user.getId())).thenReturn(false);
        when(repository.existsByEmailAndIdNot("updated@example.com", user.getId())).thenReturn(true);
        assertStatus(() -> service.update(user.getId(), request()), HttpStatus.CONFLICT);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void databaseConflictReturns409() {
        User user = existingUser();
        when(repository.saveAndFlush(user)).thenThrow(new DataIntegrityViolationException("unique"));
        assertStatus(() -> service.update(user.getId(), request()), HttpStatus.CONFLICT);
    }

    @Test
    void ldapCredentialsCannotBeChanged() {
        User user = existingUser();
        user.setLdapUser(true);
        assertStatus(() -> service.update(user.getId(), request()), HttpStatus.BAD_REQUEST);
        UpdateUserRequest request = request();
        request.setUsername(user.getUsername());
        request.setPassword("secret");
        assertStatus(() -> service.update(user.getId(), request), HttpStatus.BAD_REQUEST);
        verifyNoInteractions(encoder);
    }

    @Test
    void deleteMarksUserDeletedAndDisabled() {
        User user = existingUser();
        service.delete(user.getId());
        assertThat(user.getDeleted()).isTrue();
        assertThat(user.getEnabled()).isFalse();
        verify(repository).saveAndFlush(user);
        verify(repository, never()).delete(any(User.class));
    }

    @Test
    void searchRejectsInvalidOrSensitiveSortFields() {
        for (String field : List.of("unknown", "password", "dn")) {
            assertStatus(() -> service.search(new UserSearchRequest(), PageRequest.of(0, 20, Sort.by(field))),
                    HttpStatus.BAD_REQUEST);
        }
        verifyNoInteractions(repository);
    }

    @Test
    void passwordLimitCountsUtf8Bytes() {
        assertStatus(() -> UserService.validatePassword("é".repeat(37)), HttpStatus.BAD_REQUEST);
        assertStatus(() -> UserService.validatePassword("   "), HttpStatus.BAD_REQUEST);
        assertThatCode(() -> UserService.validatePassword("é".repeat(36))).doesNotThrowAnyException();
    }

    private User existingUser() {
        User user = user();
        when(repository.findByIdAndDeletedFalse(user.getId())).thenReturn(Optional.of(user));
        return user;
    }

    private User user() {
        User user = User.builder().username("alice").email("alice@example.com").password("old-hash")
                .role(UserRole.USER).enabled(true).deleted(false).ldapUser(false).build();
        user.setId(UUID.randomUUID());
        return user;
    }

    private UpdateUserRequest request() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("updated");
        request.setEmail("updated@example.com");
        request.setFirstName("Updated");
        request.setDepartment("IT");
        return request;
    }

    private void assertStatus(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, HttpStatus status) {
        assertThatThrownBy(action).isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode()).isEqualTo(status));
    }
}
