package com.redsaludandina.compartido.aplicacion.mensajeria;

import com.redsaludandina.compartido.dominio.AgregadoRaiz;
import com.redsaludandina.compartido.dominio.EventoDominio;
import com.redsaludandina.compartido.dominio.Requisitos;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Pasa los eventos de un agregado al outbox de su contexto. Se llama DENTRO de la unidad de
 * trabajo, después de guardar el agregado: así el estado y sus eventos se confirman juntos.
 */
public final class RegistradorOutbox {

    private final String contexto;
    private final OutboxPort outbox;
    private final Clock reloj;
    private final Supplier<UUID> generadorIds;

    public RegistradorOutbox(String contexto, OutboxPort outbox, Clock reloj) {
        this(contexto, outbox, reloj, UUID::randomUUID);
    }

    public RegistradorOutbox(String contexto, OutboxPort outbox, Clock reloj, Supplier<UUID> generadorIds) {
        this.contexto = Requisitos.texto(contexto, "contexto");
        this.outbox = Requisitos.noNulo(outbox, "outbox");
        this.reloj = Requisitos.noNulo(reloj, "reloj");
        this.generadorIds = Requisitos.noNulo(generadorIds, "generadorIds");
    }

    /** Extrae los eventos pendientes del agregado y los guarda en el outbox. */
    public void registrar(AgregadoRaiz<?> agregado) {
        registrar(String.valueOf(agregado.id()), agregado.extraerEventos());
    }

    public void registrar(String agregadoId, List<EventoDominio> eventos) {
        if (eventos.isEmpty()) {
            return;
        }
        Instant ahora = reloj.instant();
        outbox.agregar(eventos.stream()
                .map(evento -> new MensajeOutbox(generadorIds.get(), contexto, agregadoId, evento, ahora))
                .toList());
    }
}
