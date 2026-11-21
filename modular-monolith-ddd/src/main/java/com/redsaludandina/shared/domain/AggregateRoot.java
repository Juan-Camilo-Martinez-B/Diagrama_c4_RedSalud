package com.redsaludandina.shared.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Base de las raíces de agregado: acumula los eventos de dominio que produjo una operación para que
 * la capa de aplicación los guarde en el outbox junto con el agregado.
 */
public abstract class AggregateRoot<I> {

    private final List<DomainEvent> pendingEvents = new ArrayList<>();

    public abstract I id();

    protected void record(DomainEvent event) {
        pendingEvents.add(Require.notNull(event, "event"));
    }

    /** Devuelve los eventos pendientes y los vacía: cada evento se publica una sola vez. */
    public List<DomainEvent> pullEvents() {
        List<DomainEvent> events = List.copyOf(pendingEvents);
        pendingEvents.clear();
        return events;
    }
}
