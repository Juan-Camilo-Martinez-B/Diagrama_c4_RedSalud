package com.redsaludandina.compartido.dominio;

/**
 * Validaciones de entrada de los objetos de dominio. Un dato mal formado es un error del que
 * llama ({@link IllegalArgumentException}); una regla de negocio incumplida es
 * {@link ReglaDeNegocioException}.
 */
public final class Requisitos {

    private Requisitos() {
    }

    public static <T> T noNulo(T valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException(campo + " es obligatorio");
        }
        return valor;
    }

    /** Exige un texto con contenido y lo devuelve sin espacios al inicio ni al final. */
    public static String texto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " es obligatorio");
        }
        return valor.strip();
    }
}
