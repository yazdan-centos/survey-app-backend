package org.mapnaom.surveyappbackend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class TableCatalogInitializer implements ApplicationRunner {
    private static final String DDL_RESOURCE = "db/app_table_catalog.sql";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            String ddl = new ClassPathResource(DDL_RESOURCE)
                    .getContentAsString(StandardCharsets.UTF_8);
            jdbcTemplate.execute(ddl);
            log.info("Verified database table catalog table: app_table_catalog");
        } catch (IOException | RuntimeException exception) {
            log.error("Failed to initialize database table catalog table", exception);
            throw new IllegalStateException("Could not initialize app_table_catalog", exception);
        }
    }
}
