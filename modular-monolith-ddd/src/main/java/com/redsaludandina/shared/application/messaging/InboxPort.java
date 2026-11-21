package com.redsaludandina.shared.application.messaging;

import java.util.UUID;

/**
 * Registro de los eventos que un consumidor ya procesó (puerto de salida). Se usa dentro de la misma
 * transacción que aplica el efecto del evento.
 */
public interface InboxPort {

    /** {@code true} si es la primera vez que este consumidor ve el evento; {@code false} si es un duplicado. */
    boolean registerIfNew(String consumer, UUID eventId);
}
