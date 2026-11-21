package com.redsaludandina.compartido.evento;

import com.redsaludandina.compartido.dominio.EventoDominio;
import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.compartido.dominio.Requisitos;
import java.time.Instant;

/** Una interconsulta llegó a «cerrada». Analítica lo usa para la tasa de cierre. */
public record InterconsultaCerrada(
        IdClinico interconsultaId,
        String centroId,
        Instant ocurridoEn) implements EventoDominio {

    public InterconsultaCerrada {
        Requisitos.noNulo(interconsultaId, "interconsultaId");
        centroId = Requisitos.texto(centroId, "centroId");
        Requisitos.noNulo(ocurridoEn, "ocurridoEn");
    }
}
