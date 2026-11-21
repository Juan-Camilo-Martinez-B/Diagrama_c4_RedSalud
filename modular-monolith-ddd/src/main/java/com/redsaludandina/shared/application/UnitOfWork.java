package com.redsaludandina.shared.application;

import java.util.function.Supplier;

/**
 * Límite transaccional de un comando (puerto de salida). Todo lo que ocurre dentro se confirma junto
 * o no se confirma: es lo que permite guardar el agregado y su fila de outbox en la MISMA
 * transacción. La infraestructura lo implementa; la aplicación no depende del framework.
 */
public interface UnitOfWork {

    void run(Runnable work);

    /** Para leer algo que el comando necesita (por ejemplo, el agregado) dentro de una transacción. */
    <T> T call(Supplier<T> work);
}
