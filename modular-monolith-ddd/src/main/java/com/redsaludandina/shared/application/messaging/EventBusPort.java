package com.redsaludandina.shared.application.messaging;

/** Bus de eventos del dominio. Lanza una excepción si no pudo publicar. */
public interface EventBusPort {

    void publish(OutboxMessage message);
}
