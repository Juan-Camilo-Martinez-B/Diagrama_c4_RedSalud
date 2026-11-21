package com.redsaludandina.receta.dominio.modelo;

import com.redsaludandina.compartido.dominio.Requisitos;

/** Línea de la receta: qué medicamento, en qué dosis y con qué indicación. */
public record Medicamento(String nombre, String dosis, String indicacion) {

    public Medicamento {
        nombre = Requisitos.texto(nombre, "nombre del medicamento");
        dosis = Requisitos.texto(dosis, "dosis");
        indicacion = Requisitos.texto(indicacion, "indicación");
    }
}
