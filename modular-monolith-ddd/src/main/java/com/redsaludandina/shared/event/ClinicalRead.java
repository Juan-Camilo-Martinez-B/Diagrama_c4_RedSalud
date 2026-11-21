package com.redsaludandina.shared.event;

import com.redsaludandina.shared.domain.DomainEvent;
import com.redsaludandina.shared.domain.Require;
import java.time.Instant;

/**
 * Alguien leyó historia clínica. Cada lectura emite este evento con usuario, fecha, recurso y motivo,
 * y la auditoría lo guarda de forma inmutable (Caso §5). Sin motivo no hay lectura.
 */
public record ClinicalRead(
        String userId,
        String resource,
        String reason,
        Instant occurredAt) implements DomainEvent {

    public ClinicalRead {
        userId = Require.text(userId, "userId");
        resource = Require.text(resource, "resource");
        reason = Require.text(reason, "motivo de la lectura");
        Require.notNull(occurredAt, "occurredAt");
    }
}
