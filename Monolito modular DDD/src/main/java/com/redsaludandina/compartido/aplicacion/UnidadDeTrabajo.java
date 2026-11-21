package com.redsaludandina.compartido.aplicacion;

import java.util.function.Supplier;

/**
 * Límite transaccional de un caso de uso (puerto de salida). Todo lo que ocurre dentro se
 * confirma junto o no se confirma: es lo que permite guardar el agregado y su fila de outbox en
 * la MISMA transacción. La infraestructura lo implementa (por ejemplo, con la transacción de
 * Spring sobre Aurora); la aplicación no depende del framework.
 */
public interface UnidadDeTrabajo {

    <T> T ejecutar(Supplier<T> trabajo);

    default void ejecutarSinResultado(Runnable trabajo) {
        ejecutar(() -> {
            trabajo.run();
            return null;
        });
    }
}
