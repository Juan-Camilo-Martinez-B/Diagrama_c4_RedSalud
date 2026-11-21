package com.redsaludandina.shared.infrastructure.web;

/** La petición no trae un usuario autenticado. */
public class UnauthenticatedException extends RuntimeException {

    public UnauthenticatedException() {
        super("Se requiere un usuario autenticado");
    }
}
