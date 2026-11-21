package com.redsaludandina.shared.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Identificador de un agregado clínico. Es un ULID generado en el CLIENTE (el dispositivo del centro
 * rural), lo que hace idempotente la sincronización offline: reenviar el mismo registro no duplica
 * episodios (Caso §4, ADR-03).
 */
public record ClinicalId(String value) {

    /** ULID: 26 caracteres en Base32 de Crockford; el primero no supera 7. */
    private static final Pattern ULID = Pattern.compile("^[0-7][0-9A-HJKMNP-TV-Z]{25}$");

    public ClinicalId {
        value = Require.text(value, "id clínico").toUpperCase(Locale.ROOT);
        if (!ULID.matcher(value).matches()) {
            throw new IllegalArgumentException("El id clínico debe ser un ULID de 26 caracteres: " + value);
        }
    }

    public static ClinicalId of(String value) {
        return new ClinicalId(value);
    }

    /** Id opcional: {@code null} o vacío no es un id. */
    public static ClinicalId ofNullable(String value) {
        return Require.optionalText(value) == null ? null : new ClinicalId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
