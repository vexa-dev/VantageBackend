package com.vexa.vantage.delivery.infrastructure;

import com.vexa.vantage.delivery.application.BoardRepositoryPort;
import com.vexa.vantage.delivery.application.BoardService;
import com.vexa.vantage.delivery.application.ProjectMembershipProvisioningPort;
import com.vexa.vantage.delivery.application.ProjectRepositoryPort;
import com.vexa.vantage.delivery.application.ProjectRoleResolverPort;
import com.vexa.vantage.delivery.application.ProjectService;
import com.vexa.vantage.delivery.application.WorkItemRepositoryPort;
import com.vexa.vantage.delivery.application.WorkItemService;
import com.vexa.vantage.delivery.application.WorkflowDefinitionRepositoryPort;
import com.vexa.vantage.delivery.application.WorkflowDefinitionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ensambla como beans de Spring los servicios de aplicación del contexto
 * delimitado de delivery ({@link ProjectService}, {@link WorkItemService},
 * {@link BoardService}, {@link WorkflowDefinitionService}), que son clases
 * planas de Java sin anotaciones de Spring (por diseño, para mantener la
 * capa de aplicación libre de dependencias de framework) — mismo patrón que
 * {@code identity.infrastructure.IdentityBeansConfig}.
 */
@Configuration
public class DeliveryBeansConfig {

    /**
     * Nombrado explícitamente {@code "deliveryProjectService"} (mismo
     * patrón que {@code "deliveryProjectController"} en
     * {@code ProjectController}): el nombre por defecto ("projectService")
     * choca con el {@code @Service} legado
     * {@code com.vexa.vantage.service.ProjectService} (todavía sin eliminar
     * hasta el corte de Phase 8).
     */
    @Bean(name = "deliveryProjectService")
    public ProjectService projectService(
            ProjectRepositoryPort projectRepository,
            WorkflowDefinitionRepositoryPort workflowDefinitionRepository,
            ProjectRoleResolverPort projectRoleResolver,
            ProjectMembershipProvisioningPort membershipProvisioning) {
        return new ProjectService(
                projectRepository, workflowDefinitionRepository, projectRoleResolver, membershipProvisioning);
    }

    @Bean
    public WorkItemService workItemService(
            WorkItemRepositoryPort workItemRepository,
            ProjectRepositoryPort projectRepository,
            WorkflowDefinitionRepositoryPort workflowDefinitionRepository,
            ProjectRoleResolverPort projectRoleResolver) {
        return new WorkItemService(workItemRepository, projectRepository, workflowDefinitionRepository, projectRoleResolver);
    }

    @Bean
    public BoardService boardService(
            BoardRepositoryPort boardRepository,
            ProjectRepositoryPort projectRepository,
            WorkflowDefinitionRepositoryPort workflowDefinitionRepository,
            WorkItemRepositoryPort workItemRepository,
            ProjectRoleResolverPort projectRoleResolver) {
        return new BoardService(
                boardRepository, projectRepository, workflowDefinitionRepository, workItemRepository, projectRoleResolver);
    }

    @Bean
    public WorkflowDefinitionService workflowDefinitionService(
            ProjectRepositoryPort projectRepository,
            WorkflowDefinitionRepositoryPort workflowDefinitionRepository,
            ProjectRoleResolverPort projectRoleResolver) {
        return new WorkflowDefinitionService(projectRepository, workflowDefinitionRepository, projectRoleResolver);
    }
}
