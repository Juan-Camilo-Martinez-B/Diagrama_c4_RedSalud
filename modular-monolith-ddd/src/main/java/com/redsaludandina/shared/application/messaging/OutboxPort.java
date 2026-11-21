package com.redsaludandina.shared.application.messaging;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Tabla outbox de UN contexto (puerto de salida). Cada contexto tiene la suya dentro de su esquema
 * ({@code scheduling.outbox}, {@code episode.outbox}...), porque nadie escribe en el almacén de otro.
 */
public interface OutboxPort {

    /** Debe ejecutarse dentro de la misma transacción que guarda el agregado. */
    void append(List<OutboxMessage> messages);

    /** Mensajes aún no publicados, en el orden en que se registraron. */
    List<OutboxMessage> pending(int limit);

    void markPublished(UUID eventId, Instant publishedAt);
}
