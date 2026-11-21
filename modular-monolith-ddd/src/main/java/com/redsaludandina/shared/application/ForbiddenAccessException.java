package com.redsaludandina.shared.application;

/** La política de acceso (rol + atributo) negó la operación; lleva la razón de la política. */
public class ForbiddenAccessException extends RuntimeException {

    public ForbiddenAccessException(String reason) {
        super(reason);
    }
}
