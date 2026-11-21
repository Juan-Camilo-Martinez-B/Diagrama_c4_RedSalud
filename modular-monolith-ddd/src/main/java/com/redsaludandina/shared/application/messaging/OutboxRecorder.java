package com.redsaludandina.shared.application.messaging;

import com.redsaludandina.shared.domain.AggregateRoot;
import com.redsaludandina.shared.domain.DomainEvent;
import com.redsaludandina.shared.domain.Require;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Pasa los eventos de un agregado al outbox de su contexto. Se llama DENTRO de la unidad de trabajo,
 * después de guardar el agregado: así el estado y sus eventos se confirman juntos.
 */
public final class OutboxRecorder {

    private final String context;
    private final OutboxPort outbox;
    private final Clock clock;
    private final Supplier<UUID> idGenerator;

    public OutboxRecorder(String context, OutboxPort outbox, Clock clock) {
        this(context, outbox, clock, UUID::randomUUID);
    }

    public OutboxRecorder(String context, OutboxPort outbox, Clock clock, Supplier<UUID> idGenerator) {
        this.context = Require.text(context, "context");
        this.outbox = Require.notNull(outbox, "outbox");
        this.clock = Require.notNull(clock, "clock");
        this.idGenerator = Require.notNull(idGenerator, "idGenerator");
    }

    /** Extrae los eventos pendientes del agregado y los guarda en el outbox. */
    public void record(AggregateRoot<?> aggregate) {
        record(String.valueOf(aggregate.id()), aggregate.pullEvents());
    }

    public void record(String aggregateId, List<DomainEvent> events) {
        if (events.isEmpty()) {
            return;
        }
        Instant now = clock.instant();
        outbox.append(events.stream()
                .map(event -> new OutboxMessage(idGenerator.get(), context, aggregateId, event, now))
                .toList());
    }
}
