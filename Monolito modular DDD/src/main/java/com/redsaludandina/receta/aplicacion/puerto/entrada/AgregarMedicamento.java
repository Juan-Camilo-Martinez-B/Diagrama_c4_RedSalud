package com.redsaludandina.receta.aplicacion.puerto.entrada;

import com.redsaludandina.compartido.dominio.IdClinico;

/** Agrega un medicamento a una receta en borrador. Una receta firmada lo rechaza. */
public interface AgregarMedicamento {

    void agregar(Comando comando);

    record Comando(IdClinico recetaId, String nombre, String dosis, String indicacion) {
    }
}
