package com.redsaludandina.episodio.aplicacion.puerto.entrada;

import com.redsaludandina.compartido.dominio.IdClinico;

/**
 * Asocia al episodio un examen o imagen (tipo EXAMEN o IMAGEN) ya subido al almacén de adjuntos.
 */
public interface AdjuntarArchivo {

    void adjuntar(Comando comando);

    record Comando(IdClinico episodioId, IdClinico adjuntoId, String tipo, String claveAlmacenamiento,
                   String sha256) {
    }
}
