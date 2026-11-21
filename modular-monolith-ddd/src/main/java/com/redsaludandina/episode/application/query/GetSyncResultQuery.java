package com.redsaludandina.episode.application.query;

import com.redsaludandina.shared.application.cqrs.Query;

/** Respuesta R guardada para una Idempotency-Key. */
public record GetSyncResultQuery(String idempotencyKey) implements Query<SyncResultView> {
}
