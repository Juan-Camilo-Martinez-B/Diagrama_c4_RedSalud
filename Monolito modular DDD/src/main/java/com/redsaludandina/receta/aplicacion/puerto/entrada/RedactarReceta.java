package com.redsaludandina.receta.aplicacion.puerto.entrada;

import com.redsaludandina.compartido.dominio.IdClinico;

/** Abre una receta en borrador dentro de un episodio. Repetir el mismo id no la duplica. */
public interface RedactarReceta {

    void redactar(Comando comando);

    record Comando(IdClinico recetaId, IdClinico episodioId, String prescriptorId) {
    }
}
