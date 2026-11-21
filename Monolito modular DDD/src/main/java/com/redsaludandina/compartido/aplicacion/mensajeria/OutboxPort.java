package com.redsaludandina.compartido.aplicacion.mensajeria;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Tabla outbox de UN contexto (puerto de salida). Cada contexto tiene la suya dentro de su
 * esquema ({@code agenda.outbox}, {@code episodio.outbox}...), porque nadie escribe en el
 * almacén de otro.
 */
public interface OutboxPort {

    /** Debe ejecutarse dentro de la misma transacción que guarda el agregado. */
    void agregar(List<MensajeOutbox> mensajes);

    /** Mensajes aún no publicados, en el orden en que se registraron. */
    List<MensajeOutbox> pendientes(int limite);

    void marcarPublicado(UUID eventoId, Instant publicadoEn);
}
