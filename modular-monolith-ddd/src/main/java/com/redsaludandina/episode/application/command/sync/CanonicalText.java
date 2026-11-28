package com.redsaludandina.episode.application.command.sync;

/**
 * Texto canónico de un lote. El formato lo define el dominio en cada {@code toString}, para que dos
 * instancias calculen la misma huella. No usa el {@code toString} de {@code Object} ni el del record.
 */
final class CanonicalText {

    private CanonicalText() {
    }

    /** Escapa separadores para que un valor clínico no parta el campo. */
    static String field(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("|", "\\|").replace("\r", "\\r").replace("\n", "\\n");
    }

    static String join(String type, String... fields) {
        return type + "|" + String.join("|", fields);
    }
}
