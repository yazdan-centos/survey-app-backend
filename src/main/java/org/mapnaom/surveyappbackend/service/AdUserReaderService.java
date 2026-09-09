package org.mapnaom.surveyappbackend.service;

import lombok.RequiredArgsConstructor;
import org.mapnaom.surveyappbackend.config.AdProperties;
import org.mapnaom.surveyappbackend.dto.user.AdUserDto;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Service;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdUserReaderService {

    private final LdapTemplate ldapTemplate;
    private final AdProperties adProperties;

    public List<AdUserDto> readUsers() {
        return ldapTemplate.search(
                adProperties.getUserSearchBase(),
                adProperties.getUserSearchFilter(),
                (AttributesMapper<AdUserDto>) this::mapAttributes
        );
    }

    private AdUserDto mapAttributes(Attributes attributes) throws NamingException {
        return AdUserDto.builder()
                .username(getAttribute(attributes, adProperties.getUsernameAttribute()))
                .firstName(getAttribute(attributes, adProperties.getFirstNameAttribute()))
                .lastName(getAttribute(attributes, adProperties.getLastNameAttribute()))
                .displayName(getAttribute(attributes, adProperties.getDisplayNameAttribute()))
                .email(getAttribute(attributes, adProperties.getEmailAttribute()))
                .employeeId(getAttribute(attributes, adProperties.getEmployeeIdAttribute()))
                .department(getAttribute(attributes, adProperties.getDepartmentAttribute()))
                .enabled(true)
                .dn(getDn(attributes))
                .build();
    }

    private String getAttribute(Attributes attributes, String name) throws NamingException {
        if (name == null || name.isBlank()) {
            return null;
        }
        Attribute attribute = attributes.get(name);
        return attribute != null ? String.valueOf(attribute.get()) : null;
    }

    private String getDn(Attributes attributes) {
        try {
            Attribute dnAttr = attributes.get("distinguishedName");
            return dnAttr != null ? String.valueOf(dnAttr.get()) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
