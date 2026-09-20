package org.mapnaom.surveyappbackend.controller;

import lombok.RequiredArgsConstructor;
import org.mapnaom.surveyappbackend.dto.user.UserSyncResponse;
import org.mapnaom.surveyappbackend.service.UserSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserSyncController {

    private final UserSyncService userSyncService;

    @PostMapping("/sync/ad")
    public ResponseEntity<UserSyncResponse> syncUsersFromAd() {
        return ResponseEntity.ok(userSyncService.syncFromActiveDirectory());
    }
}
