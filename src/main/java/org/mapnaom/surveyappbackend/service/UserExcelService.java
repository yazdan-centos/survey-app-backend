package org.mapnaom.surveyappbackend.service;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.mapnaom.surveyappbackend.dto.user.UpdateUserRequest;
import org.mapnaom.surveyappbackend.dto.user.UserImportResponse;
import org.mapnaom.surveyappbackend.entity.User;
import org.mapnaom.surveyappbackend.entity.UserRole;
import org.mapnaom.surveyappbackend.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserExcelService {
    public static final List<String> HEADERS = List.of("username", "email", "password", "firstName",
            "lastName", "displayName", "employeeId", "department", "role", "enabled");
    private static final int MAX_ROWS = 5000;
    private static final long MAX_BYTES = 10 * 1024 * 1024;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;

    @Transactional
    public UserImportResponse importByExcel(MultipartFile file) {
        List<ImportRow> rows = readRows(file);
        Set<String> usernames = new HashSet<>();
        Set<String> emails = new HashSet<>();
        for (ImportRow row : rows) {
            UpdateUserRequest request = row.request();
            if (!usernames.add(request.getUsername()) || userRepository.existsByUsername(request.getUsername())) {
                throw rowError(HttpStatus.CONFLICT, row.number(), "username already exists");
            }
            if (request.getEmail() != null &&
                    (!emails.add(request.getEmail()) || userRepository.existsByEmail(request.getEmail()))) {
                throw rowError(HttpStatus.CONFLICT, row.number(), "email already exists");
            }
        }
        List<User> users = rows.stream().map(row -> {
            UpdateUserRequest request = row.request();
            return User.builder().username(request.getUsername()).email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .firstName(request.getFirstName()).lastName(request.getLastName())
                    .displayName(request.getDisplayName()).employeeId(request.getEmployeeId())
                    .department(request.getDepartment()).role(request.getRole())
                    .enabled(request.getEnabled()).deleted(false).ldapUser(false).build();
        }).toList();
        try {
            userRepository.saveAllAndFlush(users);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Import conflicts with an existing username or email; no users were imported", exception);
        }
        return new UserImportResponse(users.size());
    }

    public byte[] downloadWorksheetTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Users");
            sheet.createFreezePane(0, 1);
            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.size(); i++) {
                header.createCell(i).setCellValue(HEADERS.get(i));
                sheet.setDefaultColumnStyle(i, textStyle);
                sheet.setColumnWidth(i, 24 * 256);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not generate user worksheet template", exception);
        }
    }

    private List<ImportRow> readRows(MultipartFile file) {
        if (file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Excel file is empty");
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Excel file exceeds 10 MB");
        }
        try (InputStream input = file.getInputStream(); Workbook workbook = new XSSFWorkbook(input)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workbook must contain a worksheet");
            }
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getLastRowNum() > MAX_ROWS) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Import supports at most 5000 data rows");
            }
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            Map<String, Integer> columns = readHeaders(sheet.getRow(0), formatter);
            List<ImportRow> rows = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Map<String, String> values = new HashMap<>();
                for (String header : HEADERS) {
                    values.put(header, cellValue(row.getCell(columns.get(header)), formatter, i + 1,
                            !header.equals("password")));
                }
                if (values.values().stream().allMatch(Objects::isNull)) continue;
                rows.add(new ImportRow(i + 1, parseRow(values, i + 1)));
            }
            if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No user rows found");
            return rows;
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read a valid .xlsx workbook", exception);
        }
    }

    private Map<String, Integer> readHeaders(Row header, DataFormatter formatter) {
        if (header == null) throw rowError(HttpStatus.BAD_REQUEST, 1, "header row is required");
        Map<String, Integer> columns = new HashMap<>();
        for (Cell cell : header) {
            String name = cellValue(cell, formatter, 1, true);
            if (name == null) continue;
            if (!HEADERS.contains(name) || columns.putIfAbsent(name, cell.getColumnIndex()) != null) {
                throw rowError(HttpStatus.BAD_REQUEST, 1, "unknown or duplicate header; use the worksheet template");
            }
        }
        if (!columns.keySet().containsAll(HEADERS)) {
            throw rowError(HttpStatus.BAD_REQUEST, 1, "missing headers; use the worksheet template");
        }
        return columns;
    }

    private UpdateUserRequest parseRow(Map<String, String> values, int number) {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername(values.get("username"));
        request.setEmail(values.get("email"));
        request.setPassword(values.get("password"));
        request.setFirstName(values.get("firstName"));
        request.setLastName(values.get("lastName"));
        request.setDisplayName(values.get("displayName"));
        request.setEmployeeId(values.get("employeeId"));
        request.setDepartment(values.get("department"));
        try {
            request.setRole(values.get("role") == null ? UserRole.USER
                    : UserRole.valueOf(values.get("role").toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            throw rowError(HttpStatus.BAD_REQUEST, number, "role must be ADMIN, SURVEY_ADMIN or USER");
        }
        String enabled = values.get("enabled");
        if (enabled != null && !enabled.equalsIgnoreCase("true") && !enabled.equalsIgnoreCase("false")) {
            throw rowError(HttpStatus.BAD_REQUEST, number, "enabled must be true or false");
        }
        request.setEnabled(enabled == null || Boolean.parseBoolean(enabled));
        if (request.getPassword() == null) throw rowError(HttpStatus.BAD_REQUEST, number, "password is required");
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String detail = violations.stream().map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted().collect(Collectors.joining("; "));
            throw rowError(HttpStatus.BAD_REQUEST, number, detail);
        }
        try {
            UserService.validatePassword(request.getPassword());
        } catch (ResponseStatusException exception) {
            throw rowError(HttpStatus.BAD_REQUEST, number, exception.getReason());
        }
        return request;
    }

    private String cellValue(Cell cell, DataFormatter formatter, int row, boolean trim) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.FORMULA || cell.getCellType() == CellType.ERROR) {
            throw rowError(HttpStatus.BAD_REQUEST, row, "formula and error cells are not supported");
        }
        String value = formatter.formatCellValue(cell);
        if (trim) value = value.trim();
        return value.isEmpty() ? null : value;
    }

    private ResponseStatusException rowError(HttpStatus status, int row, String detail) {
        return new ResponseStatusException(status, "Row " + row + ": " + detail);
    }

    private record ImportRow(int number, UpdateUserRequest request) {
    }
}
