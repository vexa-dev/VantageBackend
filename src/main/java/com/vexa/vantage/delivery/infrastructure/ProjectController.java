package com.vexa.vantage.delivery.infrastructure;

import com.vexa.vantage.delivery.application.ProjectService;
import com.vexa.vantage.delivery.application.WorkflowDefinitionService;
import com.vexa.vantage.delivery.domain.Project;
import com.vexa.vantage.delivery.domain.ProjectStatus;
import com.vexa.vantage.delivery.domain.WorkflowDefinition;
import com.vexa.vantage.delivery.infrastructure.dto.ProjectRequest;
import com.vexa.vantage.delivery.infrastructure.dto.ProjectResponse;
import com.vexa.vantage.delivery.infrastructure.dto.UpdateProjectStatusRequest;
import com.vexa.vantage.delivery.infrastructure.dto.WorkflowDefinitionResponse;
import com.vexa.vantage.identity.domain.AppUser;
import com.vexa.vantage.identity.infrastructure.AuthenticatedActorResolver;
import com.vexa.vantage.shared.domain.ProjectId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expone los casos de uso de {@link Project}: {@code GET /api/v1/projects}
 * (listar), {@code POST /api/v1/projects} (crear), {@code GET
 * /api/v1/projects/{id}/workflow-definition} (obtener la definición de
 * workflow activa) y {@code PATCH /api/v1/projects/{id}/status} (archivar/
 * restaurar).
 *
 * <p>Resuelve la IDENTIDAD del actor autenticado con
 * {@link AuthenticatedActorResolver} (mismo patrón que
 * {@code ProjectMembershipController}/{@code UserAdminController}) y le
 * delega, junto con los datos propios de la ruta, la resolución de
 * autorización a los servicios de aplicación correspondientes.
 *
 * <p><b>Decisión flaggeada</b> (spec/diseño silenciosos): {@code GET
 * /api/v1/projects} lista los proyectos donde el actor tiene membresía, no
 * todos los proyectos del tenant — ver Javadoc de
 * {@code ProjectService#listProjects}.
 *
 * <p>Bean nombrado explícitamente {@code "deliveryProjectController"}
 * (mismo patrón que {@code identity.infrastructure.AuthController}
 * ("identityAuthController") en Phase 3): el nombre de clase por defecto
 * ("projectController") choca con el {@code @RestController} legado
 * {@code com.vexa.vantage.controller.ProjectController} (todavía sin
 * eliminar hasta el corte de Phase 8), que produciría un
 * {@code ConflictingBeanDefinitionException} al arrancar el contexto.
 */
@RestController("deliveryProjectController")
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final WorkflowDefinitionService workflowDefinitionService;
    private final AuthenticatedActorResolver actorResolver;

    public ProjectController(
            ProjectService projectService,
            WorkflowDefinitionService workflowDefinitionService,
            AuthenticatedActorResolver actorResolver) {
        this.projectService = projectService;
        this.workflowDefinitionService = workflowDefinitionService;
        this.actorResolver = actorResolver;
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> listProjects(
            @RequestParam(name = "includeArchived", defaultValue = "false") boolean includeArchived,
            Authentication authentication) {
        AppUser actor = actorResolver.resolve(authentication);
        List<Project> projects = projectService.listProjects(actor.id(), includeArchived);
        return ResponseEntity.ok(projects.stream().map(ProjectResponse::from).toList());
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody ProjectRequest request, Authentication authentication) {
        AppUser actor = actorResolver.resolve(authentication);
        Project created = projectService.createProject(actor.tenantId(), actor.id(), request.name(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectResponse.from(created));
    }

    @GetMapping("/{id}/workflow-definition")
    public ResponseEntity<WorkflowDefinitionResponse> getWorkflowDefinition(
            @PathVariable("id") String id, Authentication authentication) {
        AppUser actor = actorResolver.resolve(authentication);
        WorkflowDefinition definition = workflowDefinitionService.getByProject(ProjectId.of(id), actor.id());
        return ResponseEntity.ok(WorkflowDefinitionResponse.from(definition));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ProjectResponse> updateStatus(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateProjectStatusRequest request,
            Authentication authentication) {
        AppUser actor = actorResolver.resolve(authentication);
        ProjectId projectId = ProjectId.of(id);
        Project updated = request.status() == ProjectStatus.ARCHIVED
                ? projectService.archiveProject(projectId, actor.id())
                : projectService.restoreProject(projectId, actor.id());
        return ResponseEntity.ok(ProjectResponse.from(updated));
    }
}
