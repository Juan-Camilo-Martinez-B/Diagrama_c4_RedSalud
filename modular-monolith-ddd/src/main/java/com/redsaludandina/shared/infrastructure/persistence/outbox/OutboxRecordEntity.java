package com.redsaludandina.shared.infrastructure.persistence.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.UUID;

/**
 * Fila de la tabla outbox. Cada contexto la mapea a SU esquema con una subclase vacía
 * ({@code @Entity @Table(schema = "...", name = "outbox")}).
 */
@MappedSuperclass
public abstract class OutboxRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sequence;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(nullable = false)
    private String context;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String payload;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    void fill(UUID eventId, String context, String aggregateId, String type, String payload, Instant recordedAt) {
        this.eventId = eventId;
        this.context = context;
        this.aggregateId = aggregateId;
        this.type = type;
        this.payload = payload;
        this.recordedAt = recordedAt;
    }

    UUID eventId() {
        return eventId;
    }

    String context() {
        return context;
    }

    String aggregateId() {
        return aggregateId;
    }

    String type() {
        return type;
    }

    String payload() {
        return payload;
    }

    Instant recordedAt() {
        return recordedAt;
    }
}
