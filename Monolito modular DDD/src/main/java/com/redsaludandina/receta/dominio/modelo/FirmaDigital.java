package com.redsaludandina.receta.dominio.modelo;

import com.redsaludandina.compartido.dominio.Requisitos;
import java.time.Instant;

/** Firma del médico sobre la receta. Su presencia vuelve inmutable a la receta. */
public record FirmaDigital(String firmanteId, Instant firmadaEn, String valor) {

    public FirmaDigital {
        firmanteId = Requisitos.texto(firmanteId, "firmanteId");
        Requisitos.noNulo(firmadaEn, "firmadaEn");
        valor = Requisitos.texto(valor, "valor de la firma");
    }
}
