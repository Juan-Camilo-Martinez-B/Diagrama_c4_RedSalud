package com.redsaludandina.episodio.dominio.modelo;

import com.redsaludandina.compartido.dominio.Requisitos;
import com.redsaludandina.compartido.dominio.SelloCentro;

/** Texto clínico con el sello del centro que lo escribió. */
public record ValorClinico(String texto, SelloCentro sello) {

    public ValorClinico {
        texto = Requisitos.texto(texto, "texto clínico");
        Requisitos.noNulo(sello, "sello");
    }
}
