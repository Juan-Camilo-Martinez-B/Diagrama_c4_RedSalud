package com.redsaludandina.episode.domain.sync;

import com.redsaludandina.shared.domain.Require;
import java.util.List;

/** Respuesta R de un lote: un resultado por cambio, en el orden en que llegaron. */
public record SyncResult(List<ItemResult> items) {

    public SyncResult {
        items = List.copyOf(Require.notNull(items, "items"));
    }

    /** Definitivo si ningún cambio quedó pendiente: repetir el lote debe devolver exactamente esto. */
    public boolean definitive() {
        return items.stream().noneMatch(ItemResult::isPending);
    }
}
