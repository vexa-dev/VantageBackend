package com.vexa.vantage.delivery.infrastructure.dto;

import com.vexa.vantage.delivery.domain.TShirtSize;
import com.vexa.vantage.delivery.domain.WorkItem;

/**
 * Representación de un work item expuesta por {@code WorkItemController}/
 * {@code BoardController}. Se mapea manualmente desde {@link WorkItem}
 * (dominio puro, sin dependencia de Jackson) en lugar de serializar el
 * objeto de dominio directamente.
 *
 * <p>No incluye {@code createdAt}/{@code updatedAt}: {@link WorkItem}
 * (dominio) no los modela todavía — solo existen en
 * {@code WorkItemJpaEntity} — así que esta capa no puede exponerlos sin
 * antes extender el agregado de dominio; gap conocido, flaggeado en el
 * informe de esta fase, fuera del alcance estrictamente necesario aquí.
 */
public record WorkItemResponse(
        String id,
        String projectId,
        String parentWorkItemId,
        String sprintWorkItemId,
        String type,
        String state,
        String title,
        String description,
        Integer businessValue,
        Integer urgency,
        Integer storyPoints,
        TShirtSize tshirtSize,
        String assigneeId,
        double wsjfScore) {

    public static WorkItemResponse from(WorkItem workItem) {
        return new WorkItemResponse(
                workItem.id().value(),
                workItem.projectId().value(),
                workItem.parentWorkItemId().map(id -> id.value()).orElse(null),
                workItem.sprintWorkItemId().map(id -> id.value()).orElse(null),
                workItem.type(),
                workItem.state(),
                workItem.title(),
                workItem.description(),
                workItem.businessValue(),
                workItem.urgency(),
                workItem.storyPoints(),
                workItem.tshirtSize().orElse(null),
                workItem.assigneeId().map(id -> id.value()).orElse(null),
                workItem.wsjfScore().value());
    }
}
