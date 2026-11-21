package com.redsaludandina.shared.application.messaging;

import com.redsaludandina.shared.domain.DomainEvent;
import java.util.UUID;

/**
 * Consumidor de UN tipo de evento del lenguaje publicado. El bus entrega a cada consumidor solo los
 * eventos de su tipo, así que ningún consumidor necesita preguntar «¿qué evento es?» (abierto a
 * extensión: un evento nuevo es un consumidor nuevo, no una rama más).
 */
public interface EventConsumer<E extends DomainEvent> {

    /** Nombre estable del consumidor; es la clave de su inbox. */
    String name();

    Class<E> eventType();

    void consume(UUID eventId, E event);

    default boolean accepts(DomainEvent event) {
        return eventType().isInstance(event);
    }

    default void deliver(UUID eventId, DomainEvent event) {
        consume(eventId, eventType().cast(event));
    }
}
