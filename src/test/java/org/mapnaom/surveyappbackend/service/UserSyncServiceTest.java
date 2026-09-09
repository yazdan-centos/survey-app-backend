package org.mapnaom.surveyappbackend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapnaom.surveyappbackend.dto.user.AdUserDto;
import org.mapnaom.surveyappbackend.dto.user.UserSyncResponse;
import org.mapnaom.surveyappbackend.entity.User;
import org.mapnaom.surveyappbackend.entity.UserRole;
import org.mapnaom.surveyappbackend.repository.UserRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSyncServiceTest {
    @Mock AdUserReaderService adUserReaderService;
    @Mock UserRepository userRepository;
    @InjectMocks UserSyncService userSyncService;

    @Test
    void syncCountsCreatedUpdatedAndSkippedUsers() {
        AdUserDto created = AdUserDto.builder().username("new").email("new@example.com").enabled(true).build();
        AdUserDto skipped = AdUserDto.builder().username(" ").build();
        AdUserDto unchanged = AdUserDto.builder().username("same").email("same@example.com").enabled(true).build();
        User existing = User.builder().username("same").email("same@example.com").enabled(true).ldapUser(true).deleted(false).role(UserRole.USER).build();
        when(adUserReaderService.readUsers()).thenReturn(List.of(created, skipped, unchanged));
        when(userRepository.findByUsername("new")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("same")).thenReturn(Optional.of(existing));

        UserSyncResponse response = userSyncService.syncFromActiveDirectory();

        assertThat(response.getTotalRead()).isEqualTo(3);
        assertThat(response.getCreatedCount()).isEqualTo(1);
        assertThat(response.getUpdatedCount()).isEqualTo(0);
        assertThat(response.getSkippedCount()).isEqualTo(2);
        verify(userRepository).save(argThat(u -> "new".equals(u.getUsername()) && Boolean.TRUE.equals(u.getLdapUser())));
    }
}
