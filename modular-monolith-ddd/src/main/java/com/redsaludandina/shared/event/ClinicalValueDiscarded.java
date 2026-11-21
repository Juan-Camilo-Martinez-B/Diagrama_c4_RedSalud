package com.redsaludandina.shared.event;

import com.redsaludandina.shared.domain.CenterStamp;
import com.redsaludandina.shared.domain.ClinicalId;
import com.redsaludandina.shared.domain.DomainEvent;
import com.redsaludandina.shared.domain.Require;
import java.time.Instant;

/**
 * Last-writer-wins dejó un valor clínico fuera del estado vigente. Lleva AMBOS valores para que la
 * auditoría los registre: «no se descarta nada» (Caso §4).
 */
public record ClinicalValueDiscarded(
        ClinicalId episodeId,
        String field,
        String keptValue,
        CenterStamp keptStamp,
        String discardedValue,
        CenterStamp discardedStamp,
        Instant occurredAt) implements DomainEvent {

    public ClinicalValueDiscarded {
        Require.notNull(episodeId, "episodeId");
        field = Require.text(field, "field");
        Require.notNull(keptValue, "keptValue");
        Require.notNull(keptStamp, "keptStamp");
        Require.notNull(discardedValue, "discardedValue");
        Require.notNull(discardedStamp, "discardedStamp");
        Require.notNull(occurredAt, "occurredAt");
    }
}
