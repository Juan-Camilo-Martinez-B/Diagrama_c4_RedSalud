package com.redsaludandina.referral.infrastructure.persistence;

import com.redsaludandina.referral.domain.model.Alert;
import com.redsaludandina.referral.domain.model.AlertType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

/** Fila {@code referral.alert}. */
@Embeddable
public class AlertEmbeddable {

    @Column(nullable = false)
    private String type;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected AlertEmbeddable() {
    }

    AlertEmbeddable(Alert alert) {
        this.type = alert.type().name();
        this.createdAt = alert.createdAt();
        this.resolvedAt = alert.resolvedAt().orElse(null);
    }

    Alert toDomain() {
        return new Alert(AlertType.valueOf(type), createdAt, resolvedAt);
    }

    boolean isPending() {
        return resolvedAt == null;
    }

    String type() {
        return type;
    }
}
