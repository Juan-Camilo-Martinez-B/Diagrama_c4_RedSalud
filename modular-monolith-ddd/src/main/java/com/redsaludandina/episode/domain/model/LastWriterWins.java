package com.redsaludandina.episode.domain.model;

/**
 * Resolución last-writer-wins de un campo (Caso §4): gana el sello de centro más reciente. El valor
 * perdedor no se borra: viaja en el evento para que la auditoría guarde AMBOS.
 */
public record LastWriterWins(ClinicalValue kept, ClinicalValue discarded) {

    public static LastWriterWins between(ClinicalValue current, ClinicalValue incoming) {
        return incoming.stamp().isAfter(current.stamp())
                ? new LastWriterWins(incoming, current)
                : new LastWriterWins(current, incoming);
    }

    /** Solo hay conflicto real si los textos difieren; reescribir el mismo texto no es conflicto. */
    public boolean isConflict() {
        return !kept.text().equals(discarded.text());
    }
}
