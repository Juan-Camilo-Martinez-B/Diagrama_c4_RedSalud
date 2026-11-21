package com.redsaludandina.shared.application;

import java.util.function.Supplier;

/** Unidad de trabajo sin transacción, para probar casos de uso sin base de datos. */
public final class DirectUnitOfWork implements UnitOfWork {

    @Override
    public void run(Runnable work) {
        work.run();
    }

    @Override
    public <T> T call(Supplier<T> work) {
        return work.get();
    }
}
