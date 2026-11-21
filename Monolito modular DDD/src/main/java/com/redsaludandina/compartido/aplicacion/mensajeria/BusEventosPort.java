package com.redsaludandina.compartido.aplicacion.mensajeria;

/** Bus de eventos del dominio (EventBridge). Lanza una excepción si no pudo publicar. */
public interface BusEventosPort {

    void publicar(MensajeOutbox mensaje);
}
