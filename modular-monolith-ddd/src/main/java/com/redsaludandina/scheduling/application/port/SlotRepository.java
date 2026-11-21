package com.redsaludandina.scheduling.application.port;

import com.redsaludandina.scheduling.domain.model.Slot;
import com.redsaludandina.shared.domain.ClinicalId;
import java.util.Optional;

/** Lado de escritura: carga y guarda el agregado {@link Slot} (puerto de salida). */
public interface SlotRepository {

    boolean exists(ClinicalId slotId);

    /** Carga el cupo bloqueándolo hasta el fin de la transacción: dos reservas simultáneas se serializan. */
    Optional<Slot> findForUpdate(ClinicalId slotId);

    void save(Slot slot);
}
