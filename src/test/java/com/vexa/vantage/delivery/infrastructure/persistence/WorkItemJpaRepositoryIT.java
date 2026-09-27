package com.vexa.vantage.delivery.infrastructure.persistence;

import com.vexa.vantage.delivery.domain.TShirtSize;
import com.vexa.vantage.delivery.domain.WorkItem;
import com.vexa.vantage.identity.infrastructure.persistence.TenantJpaEntity;
import com.vexa.vantage.shared.domain.ProjectId;
import com.vexa.vantage.shared.domain.TenantId;
import com.vexa.vantage.shared.domain.WorkItemId;
import com.vexa.vantage.shared.infrastructure.TenantContext;
import com.vexa.vantage.shared.infrastructure.TenantFilterAspect;
import com.vexa.vantage.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;

/**
 * Prueba de integración (con PostgreSQL real vía Testcontainers) del mapeo
 * JPA de {@code work_item}: verifica que las dos auto-referencias
 * ({@code parent_work_item_id}/{@code sprint_work_item_id}) persisten
 * correctamente a través de {@link WorkItemRepositoryAdapter} y son
 * consultables vía las búsquedas indexadas {@code work_item(project_id,state)}
 * / {@code work_item(parent_work_item_id)} / {@code work_item(sprint_work_item_id)},
 * y que el filtro Hibernate {@code tenantFilter} (habilitado por
 * {@link TenantFilterAspect}) acota la lectura entre tenants distintos —
 * mismo patrón que {@code AppUserJpaRepositoryIT} (Phase 3).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({TenantFilterAspect.class, WorkItemRepositoryAdapter.class, WorkItemJpaRepositoryIT.AspectProxyingConfig.class})
class WorkItemJpaRepositoryIT extends PostgresTestContainerConfig {

    static class AspectProxyingConfig {
        @Bean
        static AnnotationAwareAspectJAutoProxyCreator aspectJAutoProxyCreator() {
            return new AnnotationAwareAspectJAutoProxyCreator();
        }
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private WorkItemJpaRepository rawRepository;

    @Autowired
    private WorkItemRepositoryAdapter adapter;

    private Long tenantAId;
    private Long projectAId;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    private Long persistTenant(String name) {
        return entityManager.persistFlushFind(new TenantJpaEntity(name, "FREE")).getId();
    }

    private Long persistWorkflowDefinition(Long tenantId) {
        WorkflowDefinitionJpaEntity entity = new WorkflowDefinitionJpaEntity();
        entity.setTenantId(tenantId);
        entity.setName("Scrum");
        entity.setBuiltInTemplate(false);
        entity.setStatesAndTransitions(
                "{\"version\":1,\"states\":[{\"key\":\"TODO\",\"name\":\"To Do\",\"category\":\"TODO\",\"order\":0,\"appliesTo\":[\"STORY\"]}],"
                        + "\"transitions\":[],\"initialStateByType\":{\"STORY\":\"TODO\"}}");
        return entityManager.persistFlushFind(entity).getId();
    }

    private Long persistProject(Long tenantId, Long workflowDefinitionId) {
        ProjectJpaEntity entity = new ProjectJpaEntity();
        entity.setTenantId(tenantId);
        entity.setName("Platform");
        entity.setDescription("desc");
        entity.setWorkflowDefinitionId(workflowDefinitionId);
        entity.setStatus(com.vexa.vantage.delivery.domain.ProjectStatus.ACTIVE);
        return entityManager.persistFlushFind(entity).getId();
    }

    private void seedTenantAWithOneProject() {
        tenantAId = persistTenant("Acme Corp");
        Long workflowDefinitionId = persistWorkflowDefinition(tenantAId);
        projectAId = persistProject(tenantAId, workflowDefinitionId);
        TenantContext.set(TenantId.of(String.valueOf(tenantAId)));
    }

    @Test
    void selfReferencesPersistAndAreQueryableThroughTheIndexedLookups() {
        seedTenantAWithOneProject();
        ProjectId projectId = ProjectId.of(String.valueOf(projectAId));

        WorkItem epic = new WorkItem(null, projectId, "EPIC", "TODO", "Epic 1", 50, 50, null);
        WorkItem savedEpic = adapter.save(epic);

        WorkItem story = new WorkItem(null, projectId, "STORY", "TODO", "Story 1", 30, 20, 5);
        story.changeParent(savedEpic.id());
        WorkItem savedStory = adapter.save(story);

        WorkItem sprint = new WorkItem(null, projectId, "SPRINT", "TODO", "Sprint 1", null, null, null);
        WorkItem savedSprint = adapter.save(sprint);

        // Mutate/save the RETURNED (real-id) instance, not the pre-save local `story` —
        // WorkItem.id is immutable, so re-saving the original null-id object would INSERT
        // a duplicate row instead of UPDATE-ing the persisted one (a real bug this IT
        // caught on its first RED run: findByProjectId returned 4 items, not 3).
        savedStory.assignToSprint(savedSprint.id());
        savedStory = adapter.save(savedStory);
        entityManager.flush();
        entityManager.clear();

        // work_item(project_id, state) indexed lookup
        List<WorkItem> byProject = adapter.findByProjectId(projectId);
        assertThat(byProject).extracting(WorkItem::id)
                .containsExactlyInAnyOrder(savedEpic.id(), savedStory.id(), savedSprint.id());

        // work_item(parent_work_item_id) indexed lookup, surfaced via findById
        WorkItem reloadedStory = adapter.findById(savedStory.id()).orElseThrow();
        assertThat(reloadedStory.parentWorkItemId()).contains(savedEpic.id());

        // work_item(sprint_work_item_id) indexed lookup, surfaced via findById
        assertThat(reloadedStory.sprintWorkItemId()).contains(savedSprint.id());
    }

    @Test
    void saveAlwaysStampsTenantIdFromTenantContextRegardlessOfPriorState() {
        seedTenantAWithOneProject();
        ProjectId projectId = ProjectId.of(String.valueOf(projectAId));

        WorkItem workItem = new WorkItem(null, projectId, "TASK", "TODO", "Task 1", null, null, 3);
        WorkItem saved = adapter.save(workItem);
        entityManager.flush();

        WorkItemJpaEntity persisted = rawRepository.findById(Long.parseLong(saved.id().value())).orElseThrow();
        assertThat(persisted.getTenantId()).isEqualTo(tenantAId);
    }

    @Test
    void listQueryExcludesOtherTenantsRowsButFindByIdCrossTenantGuardIsDeferredToPhase6() {
        seedTenantAWithOneProject();
        ProjectId projectAIdValue = ProjectId.of(String.valueOf(projectAId));
        WorkItem ownTenantItem = adapter.save(new WorkItem(null, projectAIdValue, "TASK", "TODO", "Own tenant task", null, null, 3));

        Long tenantBId = persistTenant("Globex Corp");
        Long workflowDefinitionBId = persistWorkflowDefinition(tenantBId);
        Long projectBId = persistProject(tenantBId, workflowDefinitionBId);
        ProjectId projectBIdValue = ProjectId.of(String.valueOf(projectBId));
        TenantContext.set(TenantId.of(String.valueOf(tenantBId)));
        WorkItem otherTenantItem = adapter.save(
                new WorkItem(null, projectBIdValue, "TASK", "TODO", "Other tenant task", null, null, 3));
        entityManager.flush();
        entityManager.clear();

        TenantContext.set(TenantId.of(String.valueOf(tenantAId)));

        // Collection query IS scoped by the Hibernate filter — confirms @Filter/@FilterDef
        // and TenantFilterAspect are correctly wired for WorkItemJpaEntity.
        assertThat(adapter.findByProjectId(projectBIdValue)).isEmpty();
        assertThat(adapter.findByProjectId(projectAIdValue)).extracting(WorkItem::id).containsExactly(ownTenantItem.id());

        // findById is genuinely NOT guarded against cross-tenant access yet: Hibernate's
        // @Filter does not apply to EntityManager.find() (the path Spring Data's findById
        // uses), which is exactly why the design reserves an explicit adapter-level
        // CrossTenantAccessException guard for Phase 6 (task 6.4) — confirmed here, not
        // just theorized. Documented as a known, deliberately deferred gap; this assertion
        // must flip to isEmpty() once Phase 6 adds the guard.
        Optional<WorkItem> foundOther = adapter.findById(otherTenantItem.id());
        assertThat(foundOther).isPresent();
    }

    @Test
    void tshirtSizeRoundTripsThroughTheDedicatedColumn() {
        seedTenantAWithOneProject();
        ProjectId projectId = ProjectId.of(String.valueOf(projectAId));

        WorkItem story = new WorkItem(null, projectId, "STORY", "TODO", "Story with estimate", 30, 20, null);
        story.changeTshirtSize(TShirtSize.L);
        WorkItem saved = adapter.save(story);
        entityManager.flush();
        entityManager.clear();

        WorkItem reloaded = adapter.findById(saved.id()).orElseThrow();
        assertThat(reloaded.tshirtSize()).contains(TShirtSize.L);
    }

    @Test
    void tshirtSizeIsEmptyWhenNeverSet() {
        seedTenantAWithOneProject();
        ProjectId projectId = ProjectId.of(String.valueOf(projectAId));

        WorkItem task = new WorkItem(null, projectId, "TASK", "TODO", "Task without estimate", null, null, 3);
        WorkItem saved = adapter.save(task);
        entityManager.flush();
        entityManager.clear();

        WorkItem reloaded = adapter.findById(saved.id()).orElseThrow();
        assertThat(reloaded.tshirtSize()).isEmpty();
    }
}
