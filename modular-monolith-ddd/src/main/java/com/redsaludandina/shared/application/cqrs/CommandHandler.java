package com.redsaludandina.shared.application.cqrs;

/**
 * Caso de uso de escritura: un manejador por comando (responsabilidad única). Trabaja sobre el
 * agregado y su repositorio; nunca devuelve datos.
 */
@FunctionalInterface
public interface CommandHandler<C extends Command> {

    void handle(C command);
}
