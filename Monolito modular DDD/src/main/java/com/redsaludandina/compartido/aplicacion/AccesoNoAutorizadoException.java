package com.redsaludandina.compartido.aplicacion;

/** La política de acceso (rol + atributo) negó la operación; lleva la razón de la política. */
public class AccesoNoAutorizadoException extends RuntimeException {

    public AccesoNoAutorizadoException(String razon) {
        super(razon);
    }
}
