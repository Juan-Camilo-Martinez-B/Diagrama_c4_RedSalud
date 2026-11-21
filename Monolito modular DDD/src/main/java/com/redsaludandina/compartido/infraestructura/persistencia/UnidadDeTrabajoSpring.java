package com.redsaludandina.compartido.infraestructura.persistencia;

import com.redsaludandina.compartido.aplicacion.RegistroDuplicadoException;
import com.redsaludandina.compartido.aplicacion.UnidadDeTrabajo;
import java.util.function.Supplier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Unidad de trabajo sobre la transacción de Spring (Aurora/PostgreSQL). Si ya hay una
 * transacción abierta se une a ella; si no, abre una. Lo que pasa dentro se confirma junto.
 *
 * <p>Una violación de restricción única se deshace y se entrega como
 * {@link RegistroDuplicadoException}: la aplicación la maneja como «ya aplicado» sin conocer JDBC.
 */
public final class UnidadDeTrabajoSpring implements UnidadDeTrabajo {

    private final TransactionTemplate transaccion;

    public UnidadDeTrabajoSpring(TransactionTemplate transaccion) {
        this.transaccion = transaccion;
    }

    @Override
    public <T> T ejecutar(Supplier<T> trabajo) {
        try {
            return transaccion.execute(estado -> trabajo.get());
        } catch (DuplicateKeyException duplicado) {
            throw new RegistroDuplicadoException("Registro duplicado por restricción única", duplicado);
        }
    }
}
