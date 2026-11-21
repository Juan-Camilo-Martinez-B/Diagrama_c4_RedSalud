package com.redsaludandina.episode.domain.model;

import com.redsaludandina.shared.domain.Require;
import java.util.Locale;

/** Tipos de adjunto clínico del caso: exámenes e imágenes. */
public enum AttachmentType {
    EXAM,
    IMAGE;

    public static AttachmentType parse(String value) {
        return valueOf(Require.text(value, "type").toUpperCase(Locale.ROOT));
    }
}
