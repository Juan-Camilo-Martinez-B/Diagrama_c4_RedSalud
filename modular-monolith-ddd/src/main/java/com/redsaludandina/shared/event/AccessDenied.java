package com.redsaludandina.shared.event;

import com.redsaludandina.shared.domain.DomainEvent;
import com.redsaludandina.shared.domain.Require;
import java.time.Instant;

/** La política de acceso negó a un usuario un recurso clínico; la auditoría guarda el intento. */
public record AccessDenied(
        String userId,
        String resource,
        String reason,
        Instant occurredAt) implements DomainEvent {

    public AccessDenied {
        userId = Require.text(userId, "userId");
        resource = Require.text(resource, "resource");
        reason = Require.text(reason, "reason");
        Require.notNull(occurredAt, "occurredAt");
    }
}
