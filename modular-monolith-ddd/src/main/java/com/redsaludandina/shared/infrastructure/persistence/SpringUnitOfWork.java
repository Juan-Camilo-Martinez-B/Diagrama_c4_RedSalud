package com.redsaludandina.shared.infrastructure.persistence;

import com.redsaludandina.shared.application.DuplicateRecordException;
import com.redsaludandina.shared.application.UnitOfWork;
import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Unidad de trabajo sobre la transacción JPA de Spring. Traduce la violación de una restricción única
 * a {@link DuplicateRecordException}, que la aplicación entiende como «ya estaba aplicado».
 */
public final class SpringUnitOfWork implements UnitOfWork {

    private final TransactionTemplate transactions;

    public SpringUnitOfWork(TransactionTemplate transactions) {
        this.transactions = transactions;
    }

    @Override
    public void run(Runnable work) {
        call(() -> {
            work.run();
            return null;
        });
    }

    @Override
    public <T> T call(Supplier<T> work) {
        try {
            return transactions.execute(status -> work.get());
        } catch (DataIntegrityViolationException duplicate) {
            throw new DuplicateRecordException("El registro ya existe: otro intento lo guardó primero", duplicate);
        }
    }
}
