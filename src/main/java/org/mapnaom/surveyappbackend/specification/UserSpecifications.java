package org.mapnaom.surveyappbackend.specification;

import jakarta.persistence.criteria.Predicate;
import org.mapnaom.surveyappbackend.dto.user.UserSearchRequest;
import org.mapnaom.surveyappbackend.entity.User;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class UserSpecifications {
    private UserSpecifications() {
    }

    public static Specification<User> search(UserSearchRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("deleted")));
            if (hasText(filter.getQ())) {
                List<Predicate> matches = new ArrayList<>();
                for (String field : List.of("username", "firstName", "lastName", "displayName",
                        "email", "employeeId", "department")) {
                    matches.add(cb.like(cb.lower(root.get(field)), contains(filter.getQ()), '\\'));
                }
                predicates.add(cb.or(matches.toArray(Predicate[]::new)));
            }
            String[] fields = {"username", "email", "department", "employeeId"};
            String[] values = {filter.getUsername(), filter.getEmail(), filter.getDepartment(), filter.getEmployeeId()};
            for (int i = 0; i < fields.length; i++) {
                if (hasText(values[i])) {
                    predicates.add(cb.like(cb.lower(root.get(fields[i])), contains(values[i]), '\\'));
                }
            }
            if (filter.getRole() != null) predicates.add(cb.equal(root.get("role"), filter.getRole()));
            if (filter.getEnabled() != null) predicates.add(cb.equal(root.get("enabled"), filter.getEnabled()));
            if (filter.getLdapUser() != null) predicates.add(cb.equal(root.get("ldapUser"), filter.getLdapUser()));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String contains(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }
}
