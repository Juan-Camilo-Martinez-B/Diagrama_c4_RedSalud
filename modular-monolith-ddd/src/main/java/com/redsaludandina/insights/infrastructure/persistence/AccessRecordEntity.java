package com.redsaludandina.insights.infrastructure.persistence;

import com.redsaludandina.insights.domain.audit.AccessRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Fila de la evidencia de accesos del perfil local. {@link Immutable}: Hibernate nunca la actualiza,
 * solo la inserta (append-only); en AWS la inmutabilidad la garantiza S3 Object Lock.
 */
@Entity
@Immutable
@Table(schema = "insights", name = "access_record")
public class AccessRecordEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String type;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String resource;

    @Column(nullable = false)
    private String reason;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(nullable = false)
    private String detail;

    protected AccessRecordEntity() {
    }

    AccessRecordEntity(AccessRecord accessRecord) {
        this.id = accessRecord.id();
        this.type = accessRecord.type().name();
        this.userId = accessRecord.userId();
        this.resource = accessRecord.resource();
        this.reason = accessRecord.reason();
        this.occurredAt = accessRecord.occurredAt();
        this.detail = accessRecord.detail();
    }
}
