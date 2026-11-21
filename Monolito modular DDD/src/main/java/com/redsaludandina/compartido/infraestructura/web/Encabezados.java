package com.redsaludandina.compartido.infraestructura.web;

/**
 * Encabezados HTTP comunes.
 *
 * <p>{@link #USUARIO} solo se acepta SIN Cognito (perfil local y pruebas). Con
 * {@code redsalud.seguridad.cognito=true} se ignora y el usuario sale del token.
 */
public final class Encabezados {

    public static final String USUARIO = "X-Usuario-Id";

    private Encabezados() {
    }
}
