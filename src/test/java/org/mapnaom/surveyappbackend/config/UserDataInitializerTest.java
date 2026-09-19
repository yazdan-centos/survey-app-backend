package org.mapnaom.surveyappbackend.config;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapnaom.surveyappbackend.entity.User;
import org.mapnaom.surveyappbackend.entity.UserRole;
import org.mapnaom.surveyappbackend.repository.UserRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDataInitializerTest {
    @Mock UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private UserDataInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new UserDataInitializer(userRepository, passwordEncoder);
        ReflectionTestUtils.setField(initializer, "usersResource", new ClassPathResource("مدیران.xlsx"));
    }

    @Test
    void importsBundledUsersWithEmailLoginAndEncodedPasswordOnlyOnce() throws Exception {
        var savedEmails = new HashSet<String>();
        when(userRepository.existsByUsername(anyString())).thenAnswer(call -> savedEmails.contains(call.getArgument(0)));
        when(userRepository.save(any(User.class))).thenAnswer(call -> {
            User user = call.getArgument(0);
            savedEmails.add(user.getEmail());
            return user;
        });

        initializer.run(null);
        initializer.run(null);

        var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(185)).save(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(user -> {
            assertThat(user.getUsername()).isEqualTo(user.getEmail());
            assertThat(passwordEncoder.matches("password123", user.getPassword())).isTrue();
            assertThat(user.getRole()).isEqualTo(UserRole.USER);
            assertThat(user.getEnabled()).isTrue();
            assertThat(user.getDeleted()).isFalse();
            assertThat(user.getLdapUser()).isFalse();
        });
        User first = captor.getAllValues().get(0);
        assertThat(first.getUsername()).isEqualTo("sinaeiniya_h@mapnaom.com");
        assertThat(first.getDisplayName()).isEqualTo("حسین سینائی نیا");
        assertThat(first.getFirstName()).isEqualTo("حسین");
        assertThat(first.getLastName()).isEqualTo("سینائی نیا");
        assertThat(first.getEmployeeId()).isEqualTo("89423");
        assertThat(first.getDepartment()).isEqualTo("پروژه تعمیرات پتروشیمی مبین");
    }

    @Test
    void preservesExistingAccountsMatchedByEmail() throws Exception {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        initializer.run(null);

        verify(userRepository, never()).save(any());
    }

    @Test
    void skipsBlankAccountsAndDuplicateRows() throws Exception {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet();
            sheet.createRow(0).createCell(4).setCellValue("Account");
            sheet.createRow(1).createCell(0).setCellValue("No email");
            sheet.createRow(2).createCell(4).setCellValue(" alice@example.com ");
            sheet.createRow(4).createCell(4).setCellValue("alice@example.com");
            workbook.write(output);
            ReflectionTestUtils.setField(initializer, "usersResource", new ByteArrayResource(output.toByteArray()));
        }

        initializer.run(null);

        var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("alice@example.com");
    }
}
