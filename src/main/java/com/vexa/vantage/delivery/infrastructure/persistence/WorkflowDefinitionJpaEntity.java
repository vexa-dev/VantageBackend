package com.vexa.vantage.delivery.infrastructure.persistence;

import com.vexa.vantage.shared.infrastructure.TenantFilterAspect;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.ParamDef;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Entidad JPA que mapea la tabla {@code workflow_definition}, separada del
 * objeto de dominio puro {@code WorkflowDefinition}.
 *
 * <p>{@code states_and_transitions} se mapea como {@code String} con
 * {@code @JdbcTypeCode(SqlTypes.JSON)} (decisión de diseño: "JSONB confined
 * to workflow_definition.states_and_transitions") — Hibernate lo trata como
 * JSON opaco de ida y vuelta contra la columna {@code jsonb}; el contenido lo
 * decodifica/codifica {@link WorkflowDefinitionJsonCodec}, nunca esta clase.
 *
 * <p>El filtro {@code tenantFilter} incluye explícitamente
 * {@code is_builtin_template = TRUE} además de la igualdad de tenant: las
 * plantillas built-in son, por diseño, datos de referencia visibles para
 * cualquier tenant (necesarias para que
 * {@code WorkflowDefinitionRepositoryPort#findBuiltInTemplate()} las
 * encuentre durante {@code ProjectService.createProject}, incluso con el
 * filtro habilitado para el tenant del actor) — de lo contrario, la fila de
 * la plantilla (propiedad del tenant de sistema) quedaría invisible tan
 * pronto el filtro esté activo para cualquier otro tenant. Documentado en el
 * informe de esta fase como decisión deliberada, no probada aún con una IT
 * (ninguna IT de este batch ejercita {@code findBuiltInTemplate()} con el
 * filtro activo).
 */
@Entity
@Table(name = "workflow_definition")
@FilterDef(name = TenantFilterAspect.TENANT_FILTER_NAME, parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = TenantFilterAspect.TENANT_FILTER_NAME,
        condition = "tenant_id = CAST(:tenantId AS BIGINT) OR is_builtin_template = TRUE")
public class WorkflowDefinitionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "is_builtin_template", nullable = false)
    private boolean builtInTemplate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "states_and_transitions", nullable = false)
    private String statesAndTransitions;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected WorkflowDefinitionJpaEntity() {
        // Requerido por JPA.
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isBuiltInTemplate() {
        return builtInTemplate;
    }

    public void setBuiltInTemplate(boolean builtInTemplate) {
        this.builtInTemplate = builtInTemplate;
    }

    public String getStatesAndTransitions() {
        return statesAndTransitions;
    }

    public void setStatesAndTransitions(String statesAndTransitions) {
        this.statesAndTransitions = statesAndTransitions;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
