package com.vexa.vantage.identity.domain;

import com.vexa.vantage.shared.domain.UserId;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que {@link ProjectMembership} empareja un {@link ProjectRole}
 * con un {@link ScrumLabel} opcional, tal como se persiste en
 * {@code project_membership(role, scrum_label)}.
 */
class ProjectMembershipTest {

    private static final UserId USER_ID = UserId.of("user-1");
    private static final String PROJECT_ID = "project-1";

    @Test
    void createsMembershipWithoutScrumLabel() {
        ProjectMembership membership = new ProjectMembership(PROJECT_ID, USER_ID, ProjectRole.MEMBER, null);

        assertThat(membership.role()).isEqualTo(ProjectRole.MEMBER);
        assertThat(membership.scrumLabel()).isEmpty();
    }

    @Test
    void createsMembershipWithScrumLabel() {
        ProjectMembership membership =
                new ProjectMembership(PROJECT_ID, USER_ID, ProjectRole.MEMBER, ScrumLabel.DEVELOPER);

        assertThat(membership.role()).isEqualTo(ProjectRole.MEMBER);
        assertThat(membership.scrumLabel()).contains(ScrumLabel.DEVELOPER);
    }

    @Test
    void ownerMembershipCanPairWithProductOwnerLabel() {
        ProjectMembership membership =
                new ProjectMembership(PROJECT_ID, USER_ID, ProjectRole.OWNER, ScrumLabel.PRODUCT_OWNER);

        assertThat(membership.role()).isEqualTo(ProjectRole.OWNER);
        assertThat(membership.scrumLabel()).isEqualTo(Optional.of(ScrumLabel.PRODUCT_OWNER));
    }
}
