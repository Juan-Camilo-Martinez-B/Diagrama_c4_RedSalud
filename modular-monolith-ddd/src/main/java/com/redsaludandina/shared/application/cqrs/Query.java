package com.redsaludandina.shared.application.cqrs;

/**
 * Pregunta sobre el estado del sistema que devuelve un modelo de lectura {@code R}. Una consulta no
 * tiene efectos: no cambia estado, no escribe en el outbox y no toca los agregados (CQRS).
 */
public interface Query<R> {
}
