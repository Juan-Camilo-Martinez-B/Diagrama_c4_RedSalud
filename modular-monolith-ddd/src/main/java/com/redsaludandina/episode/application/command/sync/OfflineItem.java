package com.redsaludandina.episode.application.command.sync;

/**
 * Un cambio del lote. {@code aggregateId} y {@code sequence} fijan el orden en que se aplican los
 * cambios de un mismo agregado (por ejemplo: abrir, registrar, cerrar).
 */
public record OfflineItem(String changeId, String aggregateId, long sequence, OfflineChange change) {

    /**
     * Forma estable entre instancias. El orden de los campos es parte del contrato de la huella.
     */
    @Override
    public String toString() {
        return CanonicalText.join("ITEM", CanonicalText.field(changeId), CanonicalText.field(aggregateId),
                Long.toString(sequence), change == null ? "" : change.toString());
    }
}
