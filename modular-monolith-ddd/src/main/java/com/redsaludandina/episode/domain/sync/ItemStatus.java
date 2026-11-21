package com.redsaludandina.episode.domain.sync;

/** Resultado de un cambio offline dentro de un lote de sincronización. */
public enum ItemStatus {
    /** Se aplicó en este intento. */
    APPLIED,
    /** Ya estaba aplicado (reintento o carrera con otro intento). */
    ALREADY_APPLIED,
    /** Una regla lo rechazó; reenviarlo igual no cambia nada. */
    REJECTED,
    /** No se pudo aplicar todavía; el cliente debe reintentar. */
    PENDING
}
