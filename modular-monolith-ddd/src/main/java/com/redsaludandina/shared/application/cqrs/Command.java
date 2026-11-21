package com.redsaludandina.shared.application.cqrs;

/**
 * Intención de cambiar el estado del sistema. Un comando no devuelve datos: si se aplica termina sin
 * resultado y, si no, lanza una excepción (CQRS).
 */
public interface Command {
}
