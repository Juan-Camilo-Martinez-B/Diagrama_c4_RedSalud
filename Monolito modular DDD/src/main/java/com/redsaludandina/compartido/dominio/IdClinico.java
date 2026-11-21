package com.redsaludandina.compartido.dominio;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Identificador de un agregado clínico. Es un ULID generado en el CLIENTE (el dispositivo del
 * centro rural), lo que hace idempotente la sincronización offline: reenviar el mismo registro
 * no duplica episodios (Caso §4, ADR-03).
 */
public record IdClinico(String valor) {

    /** ULID: 26 caracteres en Base32 de Crockford; el primero no supera 7. */
    private static final Pattern ULID = Pattern.compile("^[0-7][0-9A-HJKMNP-TV-Z]{25}$");

    public IdClinico {
        valor = Requisitos.texto(valor, "id clínico").toUpperCase(Locale.ROOT);
        if (!ULID.matcher(valor).matches()) {
            throw new IllegalArgumentException("El id clínico debe ser un ULID de 26 caracteres: " + valor);
        }
    }

    public static IdClinico de(String valor) {
        return new IdClinico(valor);
    }

    @Override
    public String toString() {
        return valor;
    }
}
