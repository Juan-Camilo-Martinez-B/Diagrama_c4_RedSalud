package com.redsaludandina.referral.domain.model;

import com.redsaludandina.shared.domain.Require;
import java.time.Instant;
import java.util.Optional;

/** Alerta de una interconsulta; queda pendiente hasta que la interconsulta avanza. */
public final class Alert {

    private final AlertType type;
    private final Instant createdAt;
    private Instant resolvedAt;

    public Alert(AlertType type, Instant createdAt, Instant resolvedAt) {
        this.type = Require.notNull(type, "type");
        this.createdAt = Require.notNull(createdAt, "createdAt");
        this.resolvedAt = resolvedAt;
    }

    static Alert open(AlertType type, Instant now) {
        return new Alert(type, now, null);
    }

    void resolve(Instant now) {
        if (resolvedAt == null) {
            resolvedAt = now;
        }
    }

    public boolean isPending() {
        return resolvedAt == null;
    }

    public AlertType type() {
        return type;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Optional<Instant> resolvedAt() {
        return Optional.ofNullable(resolvedAt);
    }
}
