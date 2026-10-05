package org.mapnaom.surveyappbackend.service;

import org.junit.jupiter.api.Test;
import org.mapnaom.surveyappbackend.dto.assignment.SaveSurveyAssignmentRequest;
import org.mapnaom.surveyappbackend.entity.*;
import org.mapnaom.surveyappbackend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
        "spring.jpa.properties.hibernate.auto_quote_keyword=true",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true"
})
@Import(SurveyAssignmentService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SurveyAssignmentReadPersistenceTest {
    @Autowired SurveyAssignmentService service;
    @Autowired UserRepository users;
    @Autowired SurveyRepository surveys;
    @Autowired SurveyAssignmentRepository assignments;

    private Survey survey() {
        Survey survey = new Survey();
        survey.setTitle("Assignment read regression");
        survey.setVersion(UUID.randomUUID().toString());
        survey.setActive(false);
        return surveys.saveAndFlush(survey);
    }

    private User user(boolean deleted) {
        User user = new User();
        user.setUsername(UUID.randomUUID().toString());
        user.setDisplayName("Assigned user");
        user.setRole(UserRole.USER);
        user.setEnabled(!deleted);
        user.setDeleted(deleted);
        user.setLdapUser(false);
        return users.saveAndFlush(user);
    }

    private Long assign(User user, Survey survey) {
        var request = new SaveSurveyAssignmentRequest();
        request.setUserIds(List.of(user.getId()));
        request.setSurveyIds(List.of(survey.getId()));
        request.setActiveFrom(Instant.parse("2000-01-01T00:00:00Z"));
        request.setActiveUntil(Instant.parse("2001-01-01T00:00:00Z"));
        return service.assign(request).get(0).getId();
    }

    @Test
    void reloadReadsCommittedAssignmentsAndDetachedUserDetailsWithoutProfileFilters() {
        Survey survey = survey();
        User activeUser = user(false);
        Long firstId = assign(activeUser, survey);
        User deletedUser = user(true);
        Long secondId = assign(deletedUser, survey);
        service.revoke(secondId);
        assign(activeUser, survey()); // Another survey must not leak into this list.

        // Every service call has its own transaction; there is no shared persistence context.
        var reloaded = service.findAssignmentsForSurvey(survey.getId());
        assertThat(reloaded).extracting(item -> item.getId()).containsExactly(secondId, firstId);
        assertThat(reloaded).allSatisfy(item -> {
            assertThat(item.getSurvey().getId()).isEqualTo(survey.getId());
            assertThat(item.getSurvey().getActive()).isFalse();
            assertThat(item.getUser().displayName()).isEqualTo("Assigned user");
            assertThat(item.getActiveUntil()).isEqualTo(Instant.parse("2001-01-01T00:00:00Z"));
        });
        assertThat(reloaded.get(0).getStatus()).isEqualTo(SurveyAssignmentStatus.REVOKED);
        assertThat(reloaded.get(0).getUser().deleted()).isTrue();
        assertThat(reloaded.get(1).getUser().deleted()).isFalse();

        // The explicit fetch plan also leaves associations usable after the repository transaction.
        var detached = assignments.findAllBySurveyIdOrderByAssignedAtDescIdDesc(survey.getId());
        assertThat(detached.get(1).getUser().getUsername()).isEqualTo(activeUser.getUsername());
        assertThat(detached.get(1).getSurvey().getTitle()).isEqualTo(survey.getTitle());
    }

    @Test
    void emptySurveyAndUnknownSurveyAreDistinguished() {
        assertThat(service.findAssignmentsForSurvey(survey().getId())).isEmpty();
        assertThatThrownBy(() -> service.findAssignmentsForSurvey(Long.MAX_VALUE))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode().value()).isEqualTo(404));
    }
}
