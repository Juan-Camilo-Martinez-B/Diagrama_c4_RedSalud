package com.redsaludandina.shared.application.messaging;

import com.redsaludandina.shared.domain.Require;
import java.time.Clock;
import java.util.HashSet;
import java.util.Set;

/**
 * Relay del outbox: publica en el bus los mensajes pendientes de un contexto y los marca como
 * publicados. Lo invoca periódicamente la infraestructura.
 *
 * <p>Conserva el orden por agregado: si falla la publicación de un mensaje, los siguientes del MISMO
 * agregado no se publican en esta vuelta (se reintentan en la próxima), pero los de otros agregados
 * sí. Si se cae entre publicar y marcar, el mensaje se publicará de nuevo: por eso los consumidores
 * toleran duplicados.
 */
public final class OutboxRelay {

    private final OutboxPort outbox;
    private final EventBusPort bus;
    private final Clock clock;

    public OutboxRelay(OutboxPort outbox, EventBusPort bus, Clock clock) {
        this.outbox = Require.notNull(outbox, "outbox");
        this.bus = Require.notNull(bus, "bus");
        this.clock = Require.notNull(clock, "clock");
    }

    public RelayResult relay(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("El límite del relevo debe ser positivo");
        }
        Set<String> blockedAggregates = new HashSet<>();
        RelayResult result = RelayResult.EMPTY;
        for (OutboxMessage message : outbox.pending(limit)) {
            result = result.plus(relayOne(message, blockedAggregates));
        }
        return result;
    }

    private RelayResult relayOne(OutboxMessage message, Set<String> blockedAggregates) {
        if (blockedAggregates.contains(message.aggregateId())) {
            return RelayResult.DEFERRED;
        }
        try {
            bus.publish(message);
            outbox.markPublished(message.eventId(), clock.instant());
            return RelayResult.PUBLISHED;
        } catch (RuntimeException failure) {
            blockedAggregates.add(message.aggregateId());
            return RelayResult.FAILED;
        }
    }

    /** Cuántos se publicaron, cuántos fallaron y cuántos se dejaron para conservar el orden. */
    public record RelayResult(int published, int failed, int deferred) {

        static final RelayResult EMPTY = new RelayResult(0, 0, 0);
        static final RelayResult PUBLISHED = new RelayResult(1, 0, 0);
        static final RelayResult FAILED = new RelayResult(0, 1, 0);
        static final RelayResult DEFERRED = new RelayResult(0, 0, 1);

        RelayResult plus(RelayResult other) {
            return new RelayResult(published + other.published, failed + other.failed, deferred + other.deferred);
        }
    }
}
