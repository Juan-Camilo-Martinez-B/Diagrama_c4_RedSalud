package com.redsaludandina.episode.application.command.sync;

/**
 * Un cambio del lote. {@code aggregateId} y {@code sequence} fijan el orden en que se aplican los
 * cambios de un mismo agregado (por ejemplo: abrir, registrar, cerrar).
 */
public record OfflineItem(String changeId, String aggregateId, long sequence, OfflineChange change) {
}
