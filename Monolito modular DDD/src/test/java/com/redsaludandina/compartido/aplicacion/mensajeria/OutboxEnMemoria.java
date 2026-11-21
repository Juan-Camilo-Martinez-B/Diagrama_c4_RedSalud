package com.redsaludandina.compartido.aplicacion.mensajeria;

import com.redsaludandina.compartido.aplicacion.Fijos;
import com.redsaludandina.compartido.dominio.EventoDominio;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Outbox de prueba en memoria, en orden de registro. */
public final class OutboxEnMemoria implements OutboxPort {

    private final List<MensajeOutbox> mensajes = new ArrayList<>();
    private final Map<UUID, Instant> publicados = new HashMap<>();
    private final Supplier<UUID> ids = Fijos.idsSecuenciales();

    /** Registrador de un contexto que escribe en este outbox con reloj e ids fijos. */
    public RegistradorOutbox registrador(String contexto) {
        return new RegistradorOutbox(contexto, this, Fijos.RELOJ, ids);
    }

    @Override
    public void agregar(List<MensajeOutbox> nuevos) {
        mensajes.addAll(nuevos);
    }

    @Override
    public List<MensajeOutbox> pendientes(int limite) {
        return mensajes.stream().filter(mensaje -> !publicados.containsKey(mensaje.eventoId())).limit(limite).toList();
    }

    @Override
    public void marcarPublicado(UUID eventoId, Instant publicadoEn) {
        publicados.put(eventoId, publicadoEn);
    }

    public List<MensajeOutbox> mensajes() {
        return List.copyOf(mensajes);
    }

    public List<EventoDominio> eventos() {
        return mensajes.stream().map(MensajeOutbox::evento).toList();
    }

    public <E> List<E> eventosDe(Class<E> tipo) {
        return mensajes.stream().map(MensajeOutbox::evento).filter(tipo::isInstance).map(tipo::cast).toList();
    }

    public boolean estaPublicado(UUID eventoId) {
        return publicados.containsKey(eventoId);
    }
}
