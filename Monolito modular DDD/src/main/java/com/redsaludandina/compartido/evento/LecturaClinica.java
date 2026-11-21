package com.redsaludandina.compartido.evento;

import com.redsaludandina.compartido.dominio.EventoDominio;
import com.redsaludandina.compartido.dominio.Requisitos;
import java.time.Instant;

/**
 * Alguien leyó historia clínica. Cada lectura emite este evento con usuario, fecha, recurso y
 * motivo, y Auditoría lo guarda de forma inmutable (Caso §5). Sin motivo no hay lectura.
 */
public record LecturaClinica(
        String usuarioId,
        String recurso,
        String motivo,
        Instant ocurridoEn) implements EventoDominio {

    public LecturaClinica {
        usuarioId = Requisitos.texto(usuarioId, "usuarioId");
        recurso = Requisitos.texto(recurso, "recurso");
        motivo = Requisitos.texto(motivo, "motivo de la lectura");
        Requisitos.noNulo(ocurridoEn, "ocurridoEn");
    }
}
