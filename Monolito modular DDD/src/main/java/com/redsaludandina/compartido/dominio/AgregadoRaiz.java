package com.redsaludandina.compartido.dominio;

import java.util.ArrayList;
import java.util.List;

/**
 * Base de las raíces de agregado: acumula los eventos de dominio que produjo una operación para
 * que la capa de aplicación los publique después de persistir el agregado.
 */
public abstract class AgregadoRaiz<ID> {

    private final List<EventoDominio> eventosPendientes = new ArrayList<>();

    public abstract ID id();

    protected void registrar(EventoDominio evento) {
        eventosPendientes.add(Requisitos.noNulo(evento, "evento"));
    }

    /** Devuelve los eventos pendientes y los vacía: cada evento se publica una sola vez. */
    public List<EventoDominio> extraerEventos() {
        List<EventoDominio> eventos = List.copyOf(eventosPendientes);
        eventosPendientes.clear();
        return eventos;
    }
}
