package com.redsaludandina.compartido.evento;

import com.redsaludandina.compartido.dominio.EventoDominio;
import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.compartido.dominio.Requisitos;
import java.time.Instant;

/** Un médico rural pidió una interconsulta. Analítica lo usa como base de la tasa de cierre. */
public record InterconsultaSolicitada(
        IdClinico interconsultaId,
        String centroId,
        String especialidad,
        Instant ocurridoEn) implements EventoDominio {

    public InterconsultaSolicitada {
        Requisitos.noNulo(interconsultaId, "interconsultaId");
        centroId = Requisitos.texto(centroId, "centroId");
        especialidad = Requisitos.texto(especialidad, "especialidad");
        Requisitos.noNulo(ocurridoEn, "ocurridoEn");
    }
}
