package org.mapnaom.surveyappbackend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    LdapContextSource ldapContextSource(@Value("${spring.ldap.urls}") String url,
                                         @Value("${spring.ldap.base}") String base,
                                         @Value("${spring.ldap.username:}") String username,
                                         @Value("${spring.ldap.password:}") String password) {
        var source = new LdapContextSource();
        source.setUrl(url); source.setBase(base);
        if (!username.isBlank()) { source.setUserDn(username); source.setPassword(password); }
        source.afterPropertiesSet();
        return source;
    }

    @Bean
    LdapAuthenticationProvider ldapAuthenticationProvider(BaseLdapPathContextSource source,
                                                          @Value("${app.security.ldap.user-dn-pattern:uid={0},ou=people}") String pattern) {
        var authenticator = new BindAuthenticator(source);
        authenticator.setUserDnPatterns(new String[]{pattern});
        return new LdapAuthenticationProvider(authenticator);
    }

    @Bean
    AuthenticationManager authenticationManager(LdapAuthenticationProvider provider) {
        return provider::authenticate;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        http.csrf(csrf -> csrf.disable()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/api/auth/login").permitAll().anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
