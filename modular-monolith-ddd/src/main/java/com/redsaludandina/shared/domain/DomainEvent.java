package com.redsaludandina.shared.domain;

import java.time.Instant;

/** Hecho del dominio que ya ocurrió y que otros contextos pueden consumir. */
public interface DomainEvent {

    Instant occurredAt();
}
