package com.redsaludandina.shared.infrastructure.aws;

import java.time.Instant;
import java.util.UUID;

/** Cuerpo JSON de un mensaje de la cola de eventos: el sobre del outbox con el evento serializado. */
public record EventEnvelope(UUID eventId, String context, String aggregateId, String type, String payload,
                            Instant recordedAt) {
}
