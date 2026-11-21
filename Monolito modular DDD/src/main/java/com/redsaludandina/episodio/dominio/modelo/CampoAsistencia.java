package com.redsaludandina.episodio.dominio.modelo;

/**
 * Campos asistenciales independientes de una teleconsulta. Cada uno se resuelve por separado con
 * last-writer-wins (Caso §4).
 */
public enum CampoAsistencia {
    MOTIVO,
    HALLAZGOS,
    CONDUCTA
}
