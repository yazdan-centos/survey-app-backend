package org.mapnaom.surveyappbackend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mapnaom.surveyappbackend.dto.user.CreateUserRequest;
import org.mapnaom.surveyappbackend.dto.user.UpdateUserRequest;
import org.mapnaom.surveyappbackend.dto.user.UserResponse;
import org.mapnaom.surveyappbackend.dto.user.UserSearchRequest;
import org.mapnaom.surveyappbackend.dto.user.UserImportResponse;
import org.mapnaom.surveyappbackend.service.UserExcelService;
import org.mapnaom.surveyappbackend.service.UserService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@CrossOrigin
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserExcelService userExcelService;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(UserResponse.from(userService.create(request)));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.findAll().stream().map(UserResponse::from).toList());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID userId) {
        return ResponseEntity.ok(UserResponse.from(userService.findById(userId)));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(UserResponse.from(userService.update(userId, request)));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        userService.delete(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<Page<UserResponse>> searchUsers(@ModelAttribute UserSearchRequest filter,
            @PageableDefault(size = 20, sort = "username") Pageable pageable) {
        return ResponseEntity.ok(userService.search(filter, pageable).map(UserResponse::from));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserImportResponse> importUsersFromExcel(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(userExcelService.importByExcel(file));
    }

    @GetMapping("/template")
    public ResponseEntity<ByteArrayResource> downloadUserImportTemplate() {
        byte[] data = userExcelService.downloadWorksheetTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users-template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(data.length)
                .body(new ByteArrayResource(data));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatusException(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(ProblemDetail.forStatusAndDetail(exception.getStatusCode(), exception.getReason()));
    }
}
