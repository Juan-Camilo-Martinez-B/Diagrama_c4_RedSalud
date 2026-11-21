package com.redsaludandina.compartido.evento;

import com.redsaludandina.compartido.dominio.EventoDominio;
import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.compartido.dominio.Requisitos;
import java.time.Instant;

/** Una receta quedó firmada e inmutable; a partir de aquí se envía a la farmacia del centro. */
public record RecetaFirmada(
        IdClinico recetaId,
        IdClinico episodioId,
        String firmanteId,
        Instant ocurridoEn) implements EventoDominio {

    public RecetaFirmada {
        Requisitos.noNulo(recetaId, "recetaId");
        Requisitos.noNulo(episodioId, "episodioId");
        firmanteId = Requisitos.texto(firmanteId, "firmanteId");
        Requisitos.noNulo(ocurridoEn, "ocurridoEn");
    }
}
