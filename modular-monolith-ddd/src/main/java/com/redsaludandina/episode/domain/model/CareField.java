package com.redsaludandina.episode.domain.model;

import com.redsaludandina.shared.domain.Require;
import java.util.Locale;

/** Campos de la nota de atención: motivo de consulta, hallazgos y conducta (plan). */
public enum CareField {
    REASON,
    FINDINGS,
    PLAN;

    public static CareField parse(String value) {
        return valueOf(Require.text(value, "field").toUpperCase(Locale.ROOT));
    }
}
