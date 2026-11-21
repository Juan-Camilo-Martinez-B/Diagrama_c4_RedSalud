package com.redsaludandina.shared.domain;

/**
 * Validaciones de entrada de los objetos de dominio. Un dato mal formado es un error del que
 * llama ({@link IllegalArgumentException}); una regla de negocio incumplida es
 * {@link BusinessRuleException}.
 */
public final class Require {

    private Require() {
    }

    public static <T> T notNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
        return value;
    }

    /** Exige un texto con contenido y lo devuelve sin espacios al inicio ni al final. */
    public static String text(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
        return value.strip();
    }

    /** Texto opcional: vacío o en blanco se normaliza a {@code null}. */
    public static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
