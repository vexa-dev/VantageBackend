package com.vexa.vantage.delivery.domain;

import com.vexa.vantage.shared.domain.DomainValidationException;
import com.vexa.vantage.shared.domain.ProjectId;
import com.vexa.vantage.shared.domain.TenantId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica {@link Project}: {@code status} por defecto {@code ACTIVE} y la
 * transición explícita de archivado/restauración
 * ({@code app_user.tenant_role} y {@code project_membership.role} tienen
 * jerarquías propias; aquí solo el ciclo de vida del propio proyecto).
 */
class ProjectTest {

    private static final ProjectId PROJECT_ID = ProjectId.of("project-1");
    private static final TenantId TENANT_ID = TenantId.of("tenant-1");
    private static final WorkflowDefinitionId WORKFLOW_ID = WorkflowDefinitionId.of("wfd-1");

    private Project newProject() {
        return new Project(PROJECT_ID, TENANT_ID, "Vantage Platform", "Rearchitecture project", WORKFLOW_ID);
    }

    @Test
    void createsProjectWithActiveStatusByDefault() {
        Project project = newProject();

        assertThat(project.status()).isEqualTo(ProjectStatus.ACTIVE);
        assertThat(project.id()).isEqualTo(PROJECT_ID);
        assertThat(project.tenantId()).isEqualTo(TENANT_ID);
        assertThat(project.workflowDefinitionId()).isEqualTo(WORKFLOW_ID);
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> new Project(PROJECT_ID, TENANT_ID, "   ", null, WORKFLOW_ID))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Project.name");
    }

    @Test
    void archiveSetsStatusToArchived() {
        Project project = newProject();

        project.archive();

        assertThat(project.status()).isEqualTo(ProjectStatus.ARCHIVED);
    }

    @Test
    void restoreSetsStatusBackToActive() {
        Project project = newProject();
        project.archive();

        project.restore();

        assertThat(project.status()).isEqualTo(ProjectStatus.ACTIVE);
    }
}
