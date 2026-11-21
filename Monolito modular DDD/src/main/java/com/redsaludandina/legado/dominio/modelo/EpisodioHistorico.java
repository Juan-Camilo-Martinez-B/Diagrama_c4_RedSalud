package com.redsaludandina.legado.dominio.modelo;

import com.redsaludandina.compartido.dominio.Requisitos;
import java.time.LocalDate;

/** Episodio del historial en el lenguaje del dominio, ya traducido desde el legado. */
public record EpisodioHistorico(String referencia, LocalDate fecha, String descripcion) {

    public EpisodioHistorico {
        referencia = Requisitos.texto(referencia, "referencia");
        Requisitos.noNulo(fecha, "fecha");
        descripcion = Requisitos.texto(descripcion, "descripcion");
    }
}
