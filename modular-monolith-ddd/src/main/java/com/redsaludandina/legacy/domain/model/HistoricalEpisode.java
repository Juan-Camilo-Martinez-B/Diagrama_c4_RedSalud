package com.redsaludandina.legacy.domain.model;

import com.redsaludandina.shared.domain.Require;
import java.time.LocalDate;

/** Atención registrada en el legado, ya traducida al modelo canónico. */
public record HistoricalEpisode(String reference, LocalDate date, String description) {

    public HistoricalEpisode {
        reference = Require.text(reference, "reference");
        Require.notNull(date, "date");
        description = Require.text(description, "description");
    }
}
