package com.redsaludandina.compartido.aplicacion.mensajeria;

import com.redsaludandina.compartido.dominio.EventoDominio;
import com.redsaludandina.compartido.dominio.Requisitos;
import java.util.UUID;

/** Evento que llega del bus a un consumidor, con su id para detectar duplicados. */
public record EventoEntrante(UUID eventoId, EventoDominio evento) {

    public EventoEntrante {
        Requisitos.noNulo(eventoId, "eventoId");
        Requisitos.noNulo(evento, "evento");
    }
}
