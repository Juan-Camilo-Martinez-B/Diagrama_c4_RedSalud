package com.redsaludandina.shared.application;

/**
 * Una restricción única de la base rechazó un registro que ya existe: otro proceso lo insertó primero
 * (por ejemplo, el mismo episodio con el mismo ULID llegando por dos reintentos a la vez). La
 * transacción se deshizo; para quien llama significa «ya estaba aplicado».
 */
public class DuplicateRecordException extends RuntimeException {

    public DuplicateRecordException(String message, Throwable cause) {
        super(message, cause);
    }
}
