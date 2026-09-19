package org.mapnaom.surveyappbackend.repository;

import org.junit.jupiter.api.Test;
import org.mapnaom.surveyappbackend.dto.user.UserSearchRequest;
import org.mapnaom.surveyappbackend.dto.user.UpdateUserRequest;
import org.mapnaom.surveyappbackend.entity.User;
import org.mapnaom.surveyappbackend.entity.UserRole;
import org.mapnaom.surveyappbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.auto_quote_keyword=true",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true"
})
class UserSearchTest {
    @Autowired UserRepository repository;

    @Test
    void combinesFiltersAndPaginatesWithCorrectTotals() {
        save("alice", "Engineering", UserRole.USER, true, false);
        save("alicia", "Engineering", UserRole.USER, true, false);
        save("alex", "Sales", UserRole.USER, true, false);
        save("albert", "Engineering", UserRole.ADMIN, true, false);
        save("alfred", "Engineering", UserRole.USER, false, false);
        save("deleted", "Engineering", UserRole.USER, true, true);
        UserSearchRequest filter = new UserSearchRequest();
        filter.setQ("AL");
        filter.setDepartment("ENGINEER");
        filter.setRole(UserRole.USER);
        filter.setEnabled(true);
        filter.setLdapUser(false);
        UserService service = service();

        var first = service.search(filter, PageRequest.of(0, 1, Sort.by("username")));
        var second = service.search(filter, PageRequest.of(1, 1, Sort.by("username")));

        assertThat(first.getTotalElements()).isEqualTo(2);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(first.getContent()).extracting(User::getUsername).containsExactly("alice");
        assertThat(second.getContent()).extracting(User::getUsername).containsExactly("alicia");
    }

    @Test
    void matchesEmailEmployeeIdAndLiteralWildcards() {
        User user = save("literal_%", "R&D", UserRole.USER, true, false);
        user.setEmail("Alice@example.com");
        user.setEmployeeId("00123");
        repository.saveAndFlush(user);
        save("literal-other", "R&D", UserRole.USER, true, false);
        UserSearchRequest filter = new UserSearchRequest();
        filter.setUsername("_%");
        filter.setEmail("ALICE@");
        filter.setEmployeeId("001");

        assertThat(service().search(filter, PageRequest.of(0, 20)).getContent())
                .extracting(User::getUsername).containsExactly("literal_%");
    }

    @Test
    void softDeleteIsPersistedAndExcludedFromEveryRead() {
        User user = save("alice", null, UserRole.USER, true, false);
        UserService service = service();
        service.delete(user.getId());

        assertThat(repository.findById(user.getId())).get().satisfies(deleted -> {
            assertThat(deleted.getDeleted()).isTrue();
            assertThat(deleted.getEnabled()).isFalse();
        });
        assertThat(repository.findByIdAndDeletedFalse(user.getId())).isEmpty();
        assertThat(service.findAll()).isEmpty();
        assertThat(service.search(new UserSearchRequest(), PageRequest.of(0, 20))).isEmpty();
    }

    @Test
    void updateCanKeepItsOwnUniqueUsernameAndEmail() {
        User user = save("alice", null, UserRole.USER, true, false);
        user.setEmail("alice@example.com");
        repository.saveAndFlush(user);
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername(user.getUsername());
        request.setEmail(user.getEmail());
        request.setDepartment("New department");

        assertThat(service().update(user.getId(), request).getDepartment()).isEqualTo("New department");
    }

    @Test
    void searchCapsPageSizeAndAddsStableSort() {
        var page = service().search(new UserSearchRequest(), PageRequest.of(0, 1000));
        assertThat(page.getSize()).isEqualTo(200);
        assertThat(page.getSort().getOrderFor("username")).isNotNull();
        assertThat(page.getSort().getOrderFor("id")).isNotNull();
    }

    private UserService service() {
        return new UserService(repository, new BCryptPasswordEncoder());
    }

    private User save(String username, String department, UserRole role, boolean enabled, boolean deleted) {
        return repository.saveAndFlush(User.builder().username(username).department(department)
                .role(role).enabled(enabled).deleted(deleted).ldapUser(false).build());
    }
}
