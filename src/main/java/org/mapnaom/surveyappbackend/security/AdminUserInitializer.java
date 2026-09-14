package org.mapnaom.surveyappbackend.security;

import lombok.RequiredArgsConstructor;
import org.mapnaom.surveyappbackend.entity.User;
import org.mapnaom.surveyappbackend.entity.UserRole;
import org.mapnaom.surveyappbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminUserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.admin.username:admin}")
    private String username;

    @Value("${app.security.admin.password:admin}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsername(username)) {
            return;
        }

        userRepository.save(User.builder()
                .username(username)
                .displayName("System Administrator")
                .password(passwordEncoder.encode(password))
                .role(UserRole.ADMIN)
                .enabled(true)
                .deleted(false)
                .ldapUser(false)
                .build());
    }
}
