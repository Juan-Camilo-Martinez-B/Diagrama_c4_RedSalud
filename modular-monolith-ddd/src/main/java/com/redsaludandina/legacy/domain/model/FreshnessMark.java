package com.redsaludandina.legacy.domain.model;

import com.redsaludandina.shared.domain.Require;
import java.time.Duration;
import java.time.Instant;

/**
 * Frescura de una copia del historial (CQRS + ACL, Caso §3): cuándo se obtuvo, cuándo caduca (TTL)
 * y de dónde salió. El legado sigue siendo la fuente de verdad.
 */
public record FreshnessMark(Instant obtainedAt, Instant expiresAt, HistorySource source) {

    public FreshnessMark {
        Require.notNull(obtainedAt, "obtainedAt");
        Require.notNull(expiresAt, "expiresAt");
        Require.notNull(source, "source");
        if (!expiresAt.isAfter(obtainedAt)) {
            throw new IllegalArgumentException("La copia del historial debe caducar después de obtenerse");
        }
    }

    public static FreshnessMark withTtl(Instant obtainedAt, Duration ttl, HistorySource source) {
        Require.notNull(ttl, "ttl");
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("El TTL del historial debe ser positivo");
        }
        return new FreshnessMark(obtainedAt, Require.notNull(obtainedAt, "obtainedAt").plus(ttl), source);
    }

    public boolean isFresh(Instant now) {
        return Require.notNull(now, "now").isBefore(expiresAt);
    }
}
