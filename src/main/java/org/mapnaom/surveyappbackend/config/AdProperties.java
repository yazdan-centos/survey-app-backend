package org.mapnaom.surveyappbackend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.ad")
@Getter
@Setter
public class AdProperties {

    private String userSearchBase;
    private String userSearchFilter;

    private String usernameAttribute;
    private String firstNameAttribute;
    private String lastNameAttribute;
    private String displayNameAttribute;
    private String emailAttribute;
    private String employeeIdAttribute;
    private String departmentAttribute;
    private String enabledAttribute;
}
