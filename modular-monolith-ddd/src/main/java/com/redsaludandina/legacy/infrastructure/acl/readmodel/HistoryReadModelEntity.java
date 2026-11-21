package com.redsaludandina.legacy.infrastructure.acl.readmodel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Fila del modelo de lectura del historial: el contenido canónico en JSON y su frescura. */
@Entity
@Table(schema = "legacy", name = "history_read_model")
public class HistoryReadModelEntity {

    @Id
    @Column(name = "patient_id")
    private String patientId;

    @Column(nullable = false)
    private String content;

    @Column(name = "obtained_at", nullable = false)
    private Instant obtainedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private String source;

    protected HistoryReadModelEntity() {
    }

    HistoryReadModelEntity(String patientId, String content, Instant obtainedAt, Instant expiresAt, String source) {
        this.patientId = patientId;
        this.content = content;
        this.obtainedAt = obtainedAt;
        this.expiresAt = expiresAt;
        this.source = source;
    }

    Instant obtainedAt() {
        return obtainedAt;
    }

    Instant expiresAt() {
        return expiresAt;
    }

    String source() {
        return source;
    }
}
