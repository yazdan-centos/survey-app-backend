package org.mapnaom.surveyappbackend.config;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.mapnaom.surveyappbackend.entity.User;
import org.mapnaom.surveyappbackend.entity.UserRole;
import org.mapnaom.surveyappbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
@org.springframework.core.annotation.Order(20)
@RequiredArgsConstructor
public class UserDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("classpath:مدیران.xlsx")
    private Resource usersResource;

    @Override
    @Transactional(rollbackFor = IOException.class)
    public void run(ApplicationArguments args) throws IOException {
        try (var input = usersResource.getInputStream(); var workbook = new XSSFWorkbook(input)) {
            var sheet = workbook.getSheetAt(0);
            var formatter = new DataFormatter(Locale.ROOT);
            if (sheet.getRow(0) == null || !"Account".equals(value(sheet.getRow(0), 4, formatter))) {
                throw new IllegalArgumentException("User seed workbook must have Account in column E");
            }
            Set<String> emails = new HashSet<>();
            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null) continue;
                String email = value(row, 4, formatter);
                if (email.isBlank() || !emails.add(email)
                        || userRepository.existsByUsername(email) || userRepository.existsByEmail(email)) {
                    continue;
                }
                String displayName = value(row, 0, formatter);
                String[] names = displayName.split("\\s+", 2);
                userRepository.save(User.builder()
                        .username(email)
                        .email(email)
                        .firstName(names[0])
                        .lastName(names.length > 1 ? names[1] : null)
                        .displayName(displayName)
                        .employeeId(value(row, 1, formatter))
                        .department(value(row, 3, formatter))
                        .role(UserRole.USER)
                        .password(passwordEncoder.encode("password123"))
                        .enabled(true)
                        .deleted(false)
                        .ldapUser(false)
                        .build());
            }
        }
    }

    private String value(Row row, int column, DataFormatter formatter) {
        return formatter.formatCellValue(row.getCell(column)).trim();
    }
}
