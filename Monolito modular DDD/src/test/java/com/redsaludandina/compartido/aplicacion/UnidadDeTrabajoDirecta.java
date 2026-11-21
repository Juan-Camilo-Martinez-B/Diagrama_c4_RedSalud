package com.redsaludandina.compartido.aplicacion;

import java.util.function.Supplier;

/** Unidad de trabajo de prueba: ejecuta en el mismo hilo y cuenta las transacciones abiertas. */
public final class UnidadDeTrabajoDirecta implements UnidadDeTrabajo {

    private int transacciones;

    @Override
    public <T> T ejecutar(Supplier<T> trabajo) {
        transacciones++;
        return trabajo.get();
    }

    public int transacciones() {
        return transacciones;
    }
}
