package com.redsaludandina.compartido.aplicacion.mensajeria;

import com.redsaludandina.compartido.dominio.EventoDominio;
import com.redsaludandina.compartido.dominio.Requisitos;
import java.time.Instant;
import java.util.UUID;

/**
 * Sobre de un evento de dominio guardado en el outbox. El {@code eventoId} es la identidad del
 * evento en todo el sistema: los consumidores lo usan para descartar duplicados.
 */
public record MensajeOutbox(UUID eventoId, String contexto, String agregadoId, EventoDominio evento,
                            Instant registradoEn) {

    public MensajeOutbox {
        Requisitos.noNulo(eventoId, "eventoId");
        contexto = Requisitos.texto(contexto, "contexto");
        agregadoId = Requisitos.texto(agregadoId, "agregadoId");
        Requisitos.noNulo(evento, "evento");
        Requisitos.noNulo(registradoEn, "registradoEn");
    }

    /** Nombre del evento en el lenguaje publicado (CupoReservado, LecturaClinica...). */
    public String tipo() {
        return evento.getClass().getSimpleName();
    }
}
