package com.redsaludandina.compartido.infraestructura.web;

/** La petición no trae una identidad válida. */
public class NoAutenticadoException extends RuntimeException {

    public NoAutenticadoException() {
        super("Se requiere un usuario autenticado");
    }
}
