package com.redsaludandina.episodio.aplicacion.puerto.entrada;

import com.redsaludandina.compartido.dominio.IdClinico;

/** Abre una teleconsulta. Abrir de nuevo el mismo id no tiene efecto (reenvío offline). */
public interface AbrirEpisodio {

    void abrir(Comando comando);

    /** {@code cupoId} es nulo si la atención no viene de la agenda. */
    record Comando(IdClinico episodioId, String pacienteId, IdClinico cupoId, String centroId) {
    }
}
