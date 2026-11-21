package com.redsaludandina.episode.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Fila {@code episode.sync_receipt}: la Idempotency-Key con la huella del lote y la respuesta R en JSON. */
@Entity
@Table(schema = "episode", name = "sync_receipt")
public class SyncReceiptEntity {

    @Id
    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "batch_hash", nullable = false)
    private String batchHash;

    @Column(nullable = false)
    private String result;

    @Column(nullable = false)
    private boolean definitive;

    @Column(nullable = false)
    private boolean replayed;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected SyncReceiptEntity() {
    }

    SyncReceiptEntity(String idempotencyKey, String batchHash, String result, boolean definitive, boolean replayed,
                      Instant recordedAt) {
        this.idempotencyKey = idempotencyKey;
        this.batchHash = batchHash;
        this.result = result;
        this.definitive = definitive;
        this.replayed = replayed;
        this.recordedAt = recordedAt;
    }

    String idempotencyKey() {
        return idempotencyKey;
    }

    String batchHash() {
        return batchHash;
    }

    String result() {
        return result;
    }

    boolean replayed() {
        return replayed;
    }

    Instant recordedAt() {
        return recordedAt;
    }
}
