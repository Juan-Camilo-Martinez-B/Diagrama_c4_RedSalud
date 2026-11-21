package com.redsaludandina.episodio.aplicacion.puerto.entrada;

import com.redsaludandina.compartido.dominio.IdClinico;
import java.time.Instant;

/**
 * Registra un campo asistencial (MOTIVO, HALLAZGOS o CONDUCTA) con el sello del centro que lo
 * escribió. Se resuelve por last-writer-wins por campo (Caso §4).
 */
public interface RegistrarAsistencia {

    void registrar(Comando comando);

    record Comando(IdClinico episodioId, String campo, String texto, String centroId, Instant selloInstante) {
    }
}
