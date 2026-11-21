package com.redsaludandina.compartido.aplicacion;

/** El caso de uso pidió un agregado que no existe. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String recurso) {
        super("No existe " + recurso);
    }
}
