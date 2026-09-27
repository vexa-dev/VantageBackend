package com.vexa.vantage.delivery.application;

import com.vexa.vantage.delivery.domain.TShirtSize;
import com.vexa.vantage.shared.domain.UserId;
import com.vexa.vantage.shared.domain.WorkItemId;

/**
 * Comando de aplicación para {@link WorkItemService#patch}: agrupa, en una
 * sola operación autorizada una sola vez, todo lo que {@code PATCH
 * /api/v1/work-items/{id}} puede modificar de un work item (transición de
 * estado, reparenting, reasignación de sprint, edición de campos y
 * reasignación de responsable).
 *
 * <p>Para {@code state}/{@code title}/{@code description}/
 * {@code businessValue}/{@code urgency}/{@code storyPoints}/{@code tshirtSize}:
 * {@code null} significa "sin cambios" — esta operación no permite volver a
 * poner {@code businessValue}/{@code urgency}/{@code storyPoints}/
 * {@code tshirtSize} en {@code null} una vez establecidos (limitación
 * deliberada y documentada, ver informe de esta fase de aplicación; ni el
 * spec ni el diseño exigen esa capacidad de "limpiar" para estos campos).
 *
 * <p>Para {@code parentWorkItemId}/{@code sprintWorkItemId}/{@code assigneeId}
 * SÍ hace falta distinguir "sin cambios" de "limpiar explícitamente a
 * {@code null}" (el diseño lo exige explícitamente para el responsable:
 * "{@code PatchWorkItemRequest} needs an explicit {@code clearAssignee}"),
 * por lo que cada uno lleva su propio indicador {@code *Provided}.
 */
public record WorkItemPatchCommand(
        String state,
        String title,
        String description,
        Integer businessValue,
        Integer urgency,
        Integer storyPoints,
        TShirtSize tshirtSize,
        boolean parentProvided,
        WorkItemId parentWorkItemId,
        boolean sprintProvided,
        WorkItemId sprintWorkItemId,
        boolean assigneeProvided,
        UserId assigneeId) {

    public static WorkItemPatchCommand empty() {
        return new WorkItemPatchCommand(
                null, null, null, null, null, null, null, false, null, false, null, false, null);
    }

    public WorkItemPatchCommand withState(String state) {
        return new WorkItemPatchCommand(
                state, title, description, businessValue, urgency, storyPoints, tshirtSize,
                parentProvided, parentWorkItemId, sprintProvided, sprintWorkItemId, assigneeProvided, assigneeId);
    }

    public WorkItemPatchCommand withTitle(String title) {
        return new WorkItemPatchCommand(
                state, title, description, businessValue, urgency, storyPoints, tshirtSize,
                parentProvided, parentWorkItemId, sprintProvided, sprintWorkItemId, assigneeProvided, assigneeId);
    }

    public WorkItemPatchCommand withDescription(String description) {
        return new WorkItemPatchCommand(
                state, title, description, businessValue, urgency, storyPoints, tshirtSize,
                parentProvided, parentWorkItemId, sprintProvided, sprintWorkItemId, assigneeProvided, assigneeId);
    }

    public WorkItemPatchCommand withPrioritization(Integer businessValue, Integer urgency, Integer storyPoints) {
        return new WorkItemPatchCommand(
                state, title, description, businessValue, urgency, storyPoints, tshirtSize,
                parentProvided, parentWorkItemId, sprintProvided, sprintWorkItemId, assigneeProvided, assigneeId);
    }

    public WorkItemPatchCommand withTshirtSize(TShirtSize tshirtSize) {
        return new WorkItemPatchCommand(
                state, title, description, businessValue, urgency, storyPoints, tshirtSize,
                parentProvided, parentWorkItemId, sprintProvided, sprintWorkItemId, assigneeProvided, assigneeId);
    }

    public WorkItemPatchCommand withParent(WorkItemId parentWorkItemId) {
        return new WorkItemPatchCommand(
                state, title, description, businessValue, urgency, storyPoints, tshirtSize,
                true, parentWorkItemId, sprintProvided, sprintWorkItemId, assigneeProvided, assigneeId);
    }

    public WorkItemPatchCommand withSprint(WorkItemId sprintWorkItemId) {
        return new WorkItemPatchCommand(
                state, title, description, businessValue, urgency, storyPoints, tshirtSize,
                parentProvided, parentWorkItemId, true, sprintWorkItemId, assigneeProvided, assigneeId);
    }

    public WorkItemPatchCommand withAssignee(UserId assigneeId) {
        return new WorkItemPatchCommand(
                state, title, description, businessValue, urgency, storyPoints, tshirtSize,
                parentProvided, parentWorkItemId, sprintProvided, sprintWorkItemId, true, assigneeId);
    }
}
