package com.vexa.vantage.delivery.application;

import com.vexa.vantage.delivery.domain.Project;
import com.vexa.vantage.delivery.domain.WorkflowDefinition;
import com.vexa.vantage.identity.domain.ProjectRole;
import com.vexa.vantage.shared.domain.ProjectId;
import com.vexa.vantage.shared.domain.TenantId;
import com.vexa.vantage.shared.domain.UserId;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de aplicación responsable de los casos de uso sobre
 * {@link Project}: creación (aprovisionando una copia propia de la plantilla
 * built-in "Scrum" insertada por {@code V2__builtin_workflow_templates.sql}
 * y asignando al creador como {@code OWNER}) y archivado/restauración.
 *
 * <p><b>{@link #createProject} NO exige un rol de proyecto</b>: el creador
 * todavía no tiene ninguna {@code ProjectMembership} en un proyecto que
 * apenas va a existir, por lo que no hay nada contra lo cual resolver
 * {@link ProjectAuthorization#requireAtLeast}. El requisito
 * "generalized-rbac" ("Default Role Assignment") dice que CUALQUIER usuario
 * autenticado puede crear un proyecto y se vuelve automáticamente su Owner —
 * el spec no es silencioso sobre quién puede crear, así que esta fase no
 * inventa una restricción de rol de tenant que el spec no exige.
 *
 * <p>{@link #createProject} es {@code @Transactional}: la fila
 * {@code project} y la fila {@code project_membership(role='OWNER')} del
 * creador se confirman (o revierten) juntas — un fallo al asignar la
 * membresía nunca deja un proyecto huérfano sin dueño. La capa de aplicación
 * es, por diseño, donde vive la orquestación de transacciones (requisito
 * "backend-hexagonal-architecture", "Application Service Orchestration");
 * {@code @Transactional} de Spring es la única anotación de framework en esta
 * clase, consistente con que el aislamiento de framework
 * ({@code ArchitectureRulesTest}) solo se exige al paquete {@code *.domain},
 * no a {@code *.application}.
 */
public class ProjectService {

    private final ProjectRepositoryPort projectRepository;
    private final WorkflowDefinitionRepositoryPort workflowDefinitionRepository;
    private final ProjectAuthorization authorization;
    private final ProjectMembershipProvisioningPort membershipProvisioning;
    private final ProjectRoleResolverPort projectRoleResolver;

    public ProjectService(
            ProjectRepositoryPort projectRepository,
            WorkflowDefinitionRepositoryPort workflowDefinitionRepository,
            ProjectRoleResolverPort projectRoleResolver,
            ProjectMembershipProvisioningPort membershipProvisioning) {
        this.projectRepository = projectRepository;
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.authorization = new ProjectAuthorization(projectRoleResolver);
        this.membershipProvisioning = membershipProvisioning;
        this.projectRoleResolver = projectRoleResolver;
    }

    /**
     * Crea un nuevo proyecto para el tenant indicado, aprovisionando una
     * copia propia (id distinto) de la plantilla built-in "Scrum" como su
     * {@code WorkflowDefinition} activa, y asigna a {@code actorId} como
     * {@code OWNER} del proyecto recién creado (requisito "generalized-rbac",
     * "Default Role Assignment").
     *
     * @throws WorkflowDefinitionNotFoundException si no existe una plantilla built-in
     */
    @Transactional
    public Project createProject(TenantId tenantId, UserId actorId, String name, String description) {
        WorkflowDefinition template = workflowDefinitionRepository.findBuiltInTemplate()
                .orElseThrow(() -> new WorkflowDefinitionNotFoundException("No built-in Scrum WorkflowDefinition template found"));

        WorkflowDefinition provisioned = workflowDefinitionRepository.save(template.withId(null));

        Project project = new Project(null, tenantId, name, description, provisioned.id());
        Project created = projectRepository.save(project);
        membershipProvisioning.provisionOwner(created.id(), actorId);
        return created;
    }

    /**
     * Archiva el proyecto indicado.
     *
     * @throws ProjectNotFoundException si {@code projectId} no existe
     * @throws com.vexa.vantage.identity.domain.InsufficientPermissionException
     *                                   si {@code actorId} no tiene al menos rol ADMIN en el proyecto
     */
    public Project archiveProject(ProjectId projectId, UserId actorId) {
        Project project = loadProjectOrThrow(projectId);
        authorization.requireAtLeast(projectId, actorId, ProjectRole.ADMIN);
        project.archive();
        return projectRepository.save(project);
    }

    /**
     * Restaura (des-archiva) el proyecto indicado — mismo mínimo de rol que
     * {@link #archiveProject}.
     *
     * @throws ProjectNotFoundException si {@code projectId} no existe
     * @throws com.vexa.vantage.identity.domain.InsufficientPermissionException
     *                                   si {@code actorId} no tiene al menos rol ADMIN en el proyecto
     */
    public Project restoreProject(ProjectId projectId, UserId actorId) {
        Project project = loadProjectOrThrow(projectId);
        authorization.requireAtLeast(projectId, actorId, ProjectRole.ADMIN);
        project.restore();
        return projectRepository.save(project);
    }

    /**
     * Lista los proyectos donde el actor indicado tiene alguna membresía de
     * proyecto (spec/diseño no especifican si "listar proyectos" significa
     * "todos los del tenant" o "los del actor" — decisión de esta fase de
     * aplicación: se interpreta como "los del actor", ver informe de esta
     * fase). Excluye proyectos archivados salvo que {@code includeArchived}
     * sea {@code true} (decidido en la resolución final de preguntas
     * abiertas: "Archived projects excluded from default list queries").
     */
    public List<Project> listProjects(UserId actorId, boolean includeArchived) {
        List<ProjectId> projectIds = projectRoleResolver.findProjectIdsForActor(actorId);
        List<Project> projects = projectRepository.findByIds(projectIds);
        if (includeArchived) {
            return projects;
        }
        return projects.stream().filter(project -> !project.isArchived()).toList();
    }

    private Project loadProjectOrThrow(ProjectId id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException("Project '" + id.value() + "' not found"));
    }
}
