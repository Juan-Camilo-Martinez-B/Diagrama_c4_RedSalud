package com.redsaludandina.shared.domain;

import java.time.Instant;

/**
 * Sello de tiempo con el que un centro registró un dato, incluso sin conexión (Caso §4). Ordena los
 * cambios para last-writer-wins; si dos centros coinciden en el instante, desempata el id del centro
 * para que el resultado sea el mismo en cualquier servidor.
 */
public record CenterStamp(String centerId, Instant instant) implements Comparable<CenterStamp> {

    public CenterStamp {
        centerId = Require.text(centerId, "centerId");
        Require.notNull(instant, "instant");
    }

    @Override
    public int compareTo(CenterStamp other) {
        int byInstant = instant.compareTo(other.instant);
        return byInstant != 0 ? byInstant : centerId.compareTo(other.centerId);
    }

    public boolean isAfter(CenterStamp other) {
        return compareTo(other) > 0;
    }
}
