package com.redsaludandina.shared.infrastructure.messaging;

import com.redsaludandina.shared.application.messaging.EventBusPort;
import com.redsaludandina.shared.application.messaging.EventConsumer;
import com.redsaludandina.shared.application.messaging.OutboxMessage;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bus de eventos dentro del proceso (perfil local y pruebas). Entrega cada evento a los consumidores
 * de su tipo; si alguno falla, el mensaje queda pendiente en el outbox y se reintenta, y los que ya
 * lo procesaron lo descartan por su inbox.
 */
public final class InProcessEventBus implements EventBusPort {

    private static final Logger LOG = LoggerFactory.getLogger(InProcessEventBus.class);

    private final List<EventConsumer<?>> consumers;

    public InProcessEventBus(List<EventConsumer<?>> consumers) {
        this.consumers = List.copyOf(consumers);
    }

    @Override
    public void publish(OutboxMessage message) {
        List<String> failed = consumers.stream()
                .filter(consumer -> consumer.accepts(message.event()))
                .filter(consumer -> !deliverSafely(consumer, message))
                .map(EventConsumer::name)
                .toList();
        if (!failed.isEmpty()) {
            throw new IllegalStateException("Consumidores con falla para " + message.eventId() + ": " + failed);
        }
    }

    private static boolean deliverSafely(EventConsumer<?> consumer, OutboxMessage message) {
        try {
            consumer.deliver(message.eventId(), message.event());
            return true;
        } catch (RuntimeException failure) {
            LOG.warn("El consumidor {} no pudo procesar {} {}: {}", consumer.name(), message.type(),
                    message.eventId(), failure.getMessage());
            return false;
        }
    }
}
