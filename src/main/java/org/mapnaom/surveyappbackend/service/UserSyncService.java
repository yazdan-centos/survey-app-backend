package org.mapnaom.surveyappbackend.service;

import lombok.RequiredArgsConstructor;
import org.mapnaom.surveyappbackend.dto.user.AdUserDto;
import org.mapnaom.surveyappbackend.dto.user.UserSyncResponse;
import org.mapnaom.surveyappbackend.entity.User;
import org.mapnaom.surveyappbackend.entity.UserRole;
import org.mapnaom.surveyappbackend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserSyncService {

    private final AdUserReaderService adUserReaderService;
    private final UserRepository userRepository;

    @Transactional
    public UserSyncResponse syncFromActiveDirectory() {
        List<AdUserDto> adUsers = adUserReaderService.readUsers();

        int created = 0;
        int updated = 0;
        int skipped = 0;

        for (AdUserDto adUser : adUsers) {
            if (adUser.getUsername() == null || adUser.getUsername().isBlank()) {
                skipped++;
                continue;
            }

            Optional<User> optionalUser = userRepository.findByUsername(adUser.getUsername());

            if (optionalUser.isPresent()) {
                User user = optionalUser.get();
                if (Boolean.TRUE.equals(user.getDeleted())) {
                    skipped++;
                    continue;
                }
                boolean changed = updateUserFields(user, adUser);
                if (changed) {
                    userRepository.save(user);
                    updated++;
                } else {
                    skipped++;
                }
            } else {
                User user = buildNewUser(adUser);
                userRepository.save(user);
                created++;
            }
        }

        return UserSyncResponse.builder()
                .totalRead(adUsers.size())
                .createdCount(created)
                .updatedCount(updated)
                .skippedCount(skipped)
                .build();
    }

    private User buildNewUser(AdUserDto adUser) {
        return User.builder()
                .username(adUser.getUsername())
                .firstName(adUser.getFirstName())
                .lastName(adUser.getLastName())
                .displayName(adUser.getDisplayName())
                .email(adUser.getEmail())
                .employeeId(adUser.getEmployeeId())
                .department(adUser.getDepartment())
                .enabled(adUser.getEnabled() != null ? adUser.getEnabled() : true)
                .deleted(false)
                .ldapUser(true)
                .dn(adUser.getDn())
                .role(UserRole.USER)
                .build();
    }

    private boolean updateUserFields(User user, AdUserDto adUser) {
        boolean changed = false;

        changed |= setIfDifferent(user.getFirstName(), adUser.getFirstName(), user::setFirstName);
        changed |= setIfDifferent(user.getLastName(), adUser.getLastName(), user::setLastName);
        changed |= setIfDifferent(user.getDisplayName(), adUser.getDisplayName(), user::setDisplayName);
        changed |= setIfDifferent(user.getEmail(), adUser.getEmail(), user::setEmail);
        changed |= setIfDifferent(user.getEmployeeId(), adUser.getEmployeeId(), user::setEmployeeId);
        changed |= setIfDifferent(user.getDepartment(), adUser.getDepartment(), user::setDepartment);
        changed |= setIfDifferent(user.getDn(), adUser.getDn(), user::setDn);

        Boolean enabled = adUser.getEnabled() != null ? adUser.getEnabled() : true;
        if (!enabled.equals(user.getEnabled())) {
            user.setEnabled(enabled);
            changed = true;
        }

        if (!Boolean.TRUE.equals(user.getLdapUser())) {
            user.setLdapUser(true);
            changed = true;
        }

        return changed;
    }

    private boolean setIfDifferent(String oldValue, String newValue, java.util.function.Consumer<String> setter) {
        if (!java.util.Objects.equals(oldValue, newValue)) {
            setter.accept(newValue);
            return true;
        }
        return false;
    }
}
