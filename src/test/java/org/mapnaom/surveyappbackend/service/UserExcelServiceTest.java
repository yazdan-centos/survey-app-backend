package org.mapnaom.surveyappbackend.service;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapnaom.surveyappbackend.entity.User;
import org.mapnaom.surveyappbackend.entity.UserRole;
import org.mapnaom.surveyappbackend.repository.UserRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserExcelServiceTest {
    @Mock UserRepository repository;
    @Mock PasswordEncoder encoder;
    private UserExcelService service;
    private ValidatorFactory validators;

    @BeforeEach
    void setUp() {
        validators = Validation.buildDefaultValidatorFactory();
        service = new UserExcelService(repository, encoder, validators.getValidator());
    }

    @AfterEach
    void close() {
        validators.close();
    }

    @Test
    void templateCanBeFilledAndImportedWithDefaultsAndEncodedPassword() throws Exception {
        when(encoder.encode(" secret ")).thenReturn("hashed");
        byte[] file = workbook(new String[]{"alice", "alice@example.com", " secret ", "Alice", "Example",
                "Alice Example", "00123", "Engineering", "", ""});

        assertThat(service.importByExcel(upload(file)).createdCount()).isEqualTo(1);

        ArgumentCaptor<List<User>> users = ArgumentCaptor.captor();
        verify(repository).saveAllAndFlush(users.capture());
        User user = users.getValue().get(0);
        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getPassword()).isEqualTo("hashed");
        assertThat(user.getEmployeeId()).isEqualTo("00123");
        assertThat(user.getDepartment()).isEqualTo("Engineering");
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getEnabled()).isTrue();
        assertThat(user.getDeleted()).isFalse();
        assertThat(user.getLdapUser()).isFalse();
    }

    @Test
    void importsExplicitRoleAndDisabledStateAndSkipsBlankRows() throws Exception {
        byte[] file = workbook(new String[]{}, new String[]{"alice", "", "secret", "", "", "", "", "", "survey_admin", "FALSE"});
        assertThat(service.importByExcel(upload(file)).createdCount()).isEqualTo(1);
        verify(repository).saveAllAndFlush(argThat(users -> {
            User user = users.iterator().next();
            return user.getRole() == UserRole.SURVEY_ADMIN && !user.getEnabled() && user.getEmail() == null;
        }));
    }

    @Test
    void invalidLaterRowPreventsAllWritesAndReportsRowNumber() throws Exception {
        byte[] file = workbook(new String[]{"alice", "alice@example.com", "secret"},
                new String[]{"bob", "invalid-email", "secret"});
        assertThatThrownBy(() -> service.importByExcel(upload(file)))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getReason()).contains("Row 3", "email").doesNotContain("secret");
                });
        verifyNoInteractions(repository, encoder);
    }

    @Test
    void rejectsDuplicateUsernamesAndEmailsWithinWorkbook() throws Exception {
        for (String[] duplicate : List.of(new String[]{"alice", "other@example.com", "secret"},
                new String[]{"bob", "alice@example.com", "secret"})) {
            byte[] file = workbook(new String[]{"alice", "alice@example.com", "secret"}, duplicate);
            assertStatus(file, HttpStatus.CONFLICT);
        }
        verify(repository, never()).saveAllAndFlush(any());
        verifyNoInteractions(encoder);
    }

    @Test
    void rejectsExistingUsersAndConcurrentDatabaseConflicts() throws Exception {
        byte[] file = workbook(new String[]{"alice", "alice@example.com", "secret"});
        when(repository.existsByUsername("alice")).thenReturn(true);
        assertStatus(file, HttpStatus.CONFLICT);
        verify(repository, never()).saveAllAndFlush(any());
        when(repository.existsByUsername("alice")).thenReturn(false);
        when(repository.saveAllAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
        assertStatus(file, HttpStatus.CONFLICT);
    }

    @Test
    void rejectsEmptyInvalidHeaderOnlyAndMissingHeaderFiles() throws Exception {
        assertStatus(new byte[0], HttpStatus.BAD_REQUEST);
        assertStatus("not excel".getBytes(), HttpStatus.BAD_REQUEST);
        assertStatus(service.downloadWorksheetTemplate(), HttpStatus.BAD_REQUEST);
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.createSheet().createRow(0).createCell(0).setCellValue("username");
            workbook.write(out);
            assertStatus(out.toByteArray(), HttpStatus.BAD_REQUEST);
        }
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsMissingPasswordsInvalidRolesAndInvalidBooleans() throws Exception {
        for (String[] row : List.of(new String[]{"alice", "", ""},
                new String[]{"alice", "", "secret", "", "", "", "", "", "invalid"},
                new String[]{"alice", "", "secret", "", "", "", "", "", "USER", "yes"})) {
            assertStatus(workbook(row), HttpStatus.BAD_REQUEST);
        }
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsFormulaCells() throws Exception {
        byte[] valid = workbook(new String[]{"alice", "", "secret"});
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(valid));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.getSheetAt(0).getRow(1).getCell(0).setCellFormula("1+1");
            workbook.write(out);
            assertStatus(out.toByteArray(), HttpStatus.BAD_REQUEST);
        }
        verifyNoInteractions(repository);
    }

    private byte[] workbook(String[]... rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(service.downloadWorksheetTemplate()));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int i = 0; i < rows.length; i++) {
                Row row = workbook.getSheetAt(0).createRow(i + 1);
                for (int j = 0; j < rows[i].length; j++) row.createCell(j).setCellValue(rows[i][j]);
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private MockMultipartFile upload(byte[] bytes) {
        return new MockMultipartFile("file", "users.xlsx", "application/octet-stream", bytes);
    }

    private void assertStatus(byte[] bytes, HttpStatus status) {
        assertThatThrownBy(() -> service.importByExcel(upload(bytes)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(status));
    }
}
