package org.mapnaom.surveyappbackend.service;

import lombok.RequiredArgsConstructor;
import org.mapnaom.surveyappbackend.dto.user.CreateUserRequest;
import org.mapnaom.surveyappbackend.dto.user.UpdateUserRequest;
import org.mapnaom.surveyappbackend.dto.user.UserSearchRequest;
import org.mapnaom.surveyappbackend.entity.User;
import org.mapnaom.surveyappbackend.entity.UserRole;
import org.mapnaom.surveyappbackend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.mapnaom.surveyappbackend.specification.UserSpecifications;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Set<String> SORT_FIELDS = Set.of("id", "username", "firstName", "lastName",
            "displayName", "email", "employeeId", "department", "role", "enabled", "ldapUser",
            "createdAt", "updatedAt");

    public List<User> findAll() {
        return userRepository.findAllByDeletedFalse();
    }

    public User findById(UUID id) {
        return userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public Page<User> search(UserSearchRequest filter, Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!SORT_FIELDS.contains(order.getProperty())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported user sort field");
            }
        }
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by("username");
        if (sort.getOrderFor("id") == null) sort = sort.and(Sort.by("id"));
        Pageable bounded = PageRequest.of(pageable.isPaged() ? pageable.getPageNumber() : 0,
                pageable.isPaged() ? Math.min(pageable.getPageSize(), 200) : 20, sort);
        return userRepository.findAll(UserSpecifications.search(filter), bounded);
    }

    @Transactional
    public User update(UUID id, UpdateUserRequest request) {
        User user = findById(id);
        if (userRepository.existsByUsernameAndIdNot(request.getUsername(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        String email = request.getEmail() == null || request.getEmail().isBlank() ? null : request.getEmail();
        if (email != null && userRepository.existsByEmailAndIdNot(email, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        if (Boolean.TRUE.equals(user.getLdapUser()) &&
                (!user.getUsername().equals(request.getUsername()) || request.getPassword() != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "LDAP usernames and passwords must be managed in the directory");
        }
        if (request.getPassword() != null) {
            validatePassword(request.getPassword());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        user.setUsername(request.getUsername());
        user.setEmail(email);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setDisplayName(request.getDisplayName());
        user.setEmployeeId(request.getEmployeeId());
        user.setDepartment(request.getDepartment());
        if (request.getRole() != null) user.setRole(request.getRole());
        if (request.getEnabled() != null) user.setEnabled(request.getEnabled());
        return save(user);
    }

    @Transactional
    public void delete(UUID id) {
        User user = findById(id);
        user.setDeleted(true);
        user.setEnabled(false);
        save(user);
    }

    private User save(User user) {
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Username or email conflicts with an existing user", exception);
        }
    }

    static void validatePassword(String password) {
        if (password.isBlank() || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must not be blank and must be at most 72 UTF-8 bytes");
        }
    }

    public User create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : UserRole.USER)
                .enabled(true)
                .deleted(false)
                .ldapUser(false)
                .build();

        return userRepository.save(user);
    }
}
