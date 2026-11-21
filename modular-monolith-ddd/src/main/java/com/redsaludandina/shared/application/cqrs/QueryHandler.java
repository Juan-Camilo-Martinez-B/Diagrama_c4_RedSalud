package com.redsaludandina.shared.application.cqrs;

/**
 * Caso de uso de lectura: un manejador por consulta. Lee de un puerto de lectura que entrega DTO
 * propios del modelo de lectura, sin cargar agregados del dominio.
 */
@FunctionalInterface
public interface QueryHandler<Q extends Query<R>, R> {

    R handle(Q query);
}
