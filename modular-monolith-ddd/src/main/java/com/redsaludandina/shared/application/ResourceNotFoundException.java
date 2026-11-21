package com.redsaludandina.shared.application;

/** El caso de uso pidió algo que no existe. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource) {
        super("No existe " + resource);
    }
}
