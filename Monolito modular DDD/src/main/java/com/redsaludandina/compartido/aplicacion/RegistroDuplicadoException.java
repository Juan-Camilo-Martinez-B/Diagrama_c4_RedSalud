package com.redsaludandina.compartido.aplicacion;

/**
 * Una restricción única de la base rechazó un registro que ya existe: otro proceso lo insertó
 * primero (por ejemplo, el mismo episodio con el mismo ULID llegando por dos reintentos a la vez).
 * La transacción se deshizo; para quien llama significa «ya estaba aplicado».
 */
public class RegistroDuplicadoException extends RuntimeException {

    public RegistroDuplicadoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
