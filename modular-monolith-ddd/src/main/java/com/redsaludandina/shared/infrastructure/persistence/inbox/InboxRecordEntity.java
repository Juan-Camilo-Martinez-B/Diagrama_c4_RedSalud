package com.redsaludandina.shared.infrastructure.persistence.inbox;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.UUID;

/**
 * Fila del inbox: «este consumidor ya procesó este evento». Cada contexto consumidor la mapea a SU
 * esquema con una subclase vacía.
 */
@MappedSuperclass
public abstract class InboxRecordEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String consumer;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    static String idOf(String consumer, UUID eventId) {
        return consumer + "|" + eventId;
    }

    void fill(String consumer, UUID eventId, Instant receivedAt) {
        this.id = idOf(consumer, eventId);
        this.consumer = consumer;
        this.eventId = eventId;
        this.receivedAt = receivedAt;
    }
}
