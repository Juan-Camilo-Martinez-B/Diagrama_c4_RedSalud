package com.redsaludandina.legacy.domain.model;

import com.redsaludandina.shared.domain.Require;
import java.time.LocalDate;

/** Documento disponible en el legado (laboratorio, imagen...), ya traducido al modelo canónico. */
public record AvailableDocument(String reference, String type, LocalDate date) {

    public AvailableDocument {
        reference = Require.text(reference, "reference");
        type = Require.text(type, "type");
        Require.notNull(date, "date");
    }
}
