package com.redsaludandina.compartido.evento;

import com.redsaludandina.compartido.dominio.EventoDominio;
import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.compartido.dominio.Requisitos;
import java.time.Instant;

/**
 * Agenda reservó un cupo. Interconsulta lo usa para pasar a «agendada» y Analítica para medir el
 * tiempo de espera ({@code inicioFranja - ocurridoEn}).
 */
public record CupoReservado(
        IdClinico cupoId,
        IdClinico reservaId,
        String pacienteId,
        String centroId,
        String especialistaId,
        Instant inicioFranja,
        Instant ocurridoEn) implements EventoDominio {

    public CupoReservado {
        Requisitos.noNulo(cupoId, "cupoId");
        Requisitos.noNulo(reservaId, "reservaId");
        pacienteId = Requisitos.texto(pacienteId, "pacienteId");
        centroId = Requisitos.texto(centroId, "centroId");
        especialistaId = Requisitos.texto(especialistaId, "especialistaId");
        Requisitos.noNulo(inicioFranja, "inicioFranja");
        Requisitos.noNulo(ocurridoEn, "ocurridoEn");
    }
}
