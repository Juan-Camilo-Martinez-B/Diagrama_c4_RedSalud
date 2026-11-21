package com.redsaludandina.compartido.evento;

import com.redsaludandina.compartido.dominio.EventoDominio;
import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.compartido.dominio.Requisitos;
import java.time.Instant;
import java.util.Optional;

/**
 * Episodio clínico terminó la teleconsulta. Interconsulta lo usa para pasar a «atendida» y
 * Analítica para contar teleconsultas por centro. {@code cupoId} es nulo si la atención no vino
 * de un cupo de la agenda.
 */
public record PacienteAtendido(
        IdClinico episodioId,
        String pacienteId,
        IdClinico cupoId,
        String centroId,
        Instant ocurridoEn) implements EventoDominio {

    public PacienteAtendido {
        Requisitos.noNulo(episodioId, "episodioId");
        pacienteId = Requisitos.texto(pacienteId, "pacienteId");
        centroId = Requisitos.texto(centroId, "centroId");
        Requisitos.noNulo(ocurridoEn, "ocurridoEn");
    }

    public Optional<IdClinico> cupo() {
        return Optional.ofNullable(cupoId);
    }
}
