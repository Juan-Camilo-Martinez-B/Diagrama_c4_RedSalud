package com.redsaludandina.shared.application.messaging;

import com.redsaludandina.shared.domain.DomainEvent;
import com.redsaludandina.shared.domain.Require;
import java.time.Instant;
import java.util.UUID;

/**
 * Sobre de un evento de dominio guardado en el outbox. El {@code eventId} es la identidad del evento
 * en todo el sistema: los consumidores lo usan para descartar duplicados.
 */
public record OutboxMessage(UUID eventId, String context, String aggregateId, DomainEvent event,
                            Instant recordedAt) {

    public OutboxMessage {
        Require.notNull(eventId, "eventId");
        context = Require.text(context, "context");
        aggregateId = Require.text(aggregateId, "aggregateId");
        Require.notNull(event, "event");
        Require.notNull(recordedAt, "recordedAt");
    }

    /** Nombre del evento en el lenguaje publicado (SlotBooked, ClinicalRead...). */
    public String type() {
        return event.getClass().getSimpleName();
    }
}
