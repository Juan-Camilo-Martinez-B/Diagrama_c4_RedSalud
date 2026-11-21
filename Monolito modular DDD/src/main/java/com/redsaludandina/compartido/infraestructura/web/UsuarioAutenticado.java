package com.redsaludandina.compartido.infraestructura.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca el parámetro de un controlador que recibe el id del usuario autenticado. Con Cognito es
 * el {@code sub} del token JWT; nunca un valor que el cliente declare en el cuerpo.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface UsuarioAutenticado {
}
