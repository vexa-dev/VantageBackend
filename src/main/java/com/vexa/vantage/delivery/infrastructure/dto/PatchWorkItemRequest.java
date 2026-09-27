package com.vexa.vantage.delivery.infrastructure.dto;

import com.vexa.vantage.delivery.application.WorkItemPatchCommand;
import com.vexa.vantage.delivery.domain.TShirtSize;
import com.vexa.vantage.shared.domain.UserId;
import com.vexa.vantage.shared.domain.WorkItemId;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Cuerpo de la solicitud de {@code PATCH /api/v1/work-items/{id}}: cubre
 * transición de estado, reparenting, reasignación de sprint, edición de
 * campos y reasignación de responsable en una sola solicitud.
 *
 * <p>Para {@code state}/{@code title}/{@code description}/
 * {@code businessValue}/{@code urgency}/{@code storyPoints}/{@code tshirtSize}:
 * {@code null} (u omitido) significa "sin cambios" — esta operación no
 * permite volver a poner estos campos en {@code null} una vez establecidos
 * (limitación deliberada, ver Javadoc de {@link WorkItemPatchCommand}).
 *
 * <p>Para {@code parentWorkItemId}/{@code sprintWorkItemId}/{@code assigneeId}
 * SÍ hace falta distinguir "sin cambios" de "limpiar explícitamente" — cada
 * uno lleva su propio indicador explícito ({@code clearParent}/
 * {@code clearSprint}/{@code clearAssignee}), igual que el diseño exige
 * explícitamente para el responsable ("{@code PatchWorkItemRequest} needs an
 * explicit {@code clearAssignee}: {@code Boolean}"). Se usa {@link Boolean}
 * (objeto), no {@code boolean} primitivo: Jackson 3 rechaza un JSON que
 * omite por completo un campo primitivo del constructor canónico de un
 * record con {@code MismatchedInputException: Cannot map null into type
 * boolean} — un {@link Boolean} ausente se resuelve a {@code null}, tratado
 * como {@code false} en {@link #toCommand()}.
 */
public record PatchWorkItemRequest(
        String state,
        String title,
        String description,
        @Min(0) @Max(100) Integer businessValue,
        @Min(0) @Max(100) Integer urgency,
        @Min(0) Integer storyPoints,
        TShirtSize tshirtSize,
        String parentWorkItemId,
        Boolean clearParent,
        String sprintWorkItemId,
        Boolean clearSprint,
        String assigneeId,
        Boolean clearAssignee) {

    public WorkItemPatchCommand toCommand() {
        WorkItemPatchCommand command = WorkItemPatchCommand.empty()
                .withState(state)
                .withTitle(title)
                .withDescription(description)
                .withPrioritization(businessValue, urgency, storyPoints)
                .withTshirtSize(tshirtSize);

        if (parentWorkItemId != null) {
            command = command.withParent(WorkItemId.of(parentWorkItemId));
        } else if (Boolean.TRUE.equals(clearParent)) {
            command = command.withParent(null);
        }

        if (sprintWorkItemId != null) {
            command = command.withSprint(WorkItemId.of(sprintWorkItemId));
        } else if (Boolean.TRUE.equals(clearSprint)) {
            command = command.withSprint(null);
        }

        if (assigneeId != null) {
            command = command.withAssignee(UserId.of(assigneeId));
        } else if (Boolean.TRUE.equals(clearAssignee)) {
            command = command.withAssignee(null);
        }

        return command;
    }
}
