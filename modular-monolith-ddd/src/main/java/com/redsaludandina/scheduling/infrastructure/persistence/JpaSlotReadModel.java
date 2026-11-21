package com.redsaludandina.scheduling.infrastructure.persistence;

import com.redsaludandina.scheduling.application.port.SlotReadModel;
import com.redsaludandina.scheduling.application.query.AvailableSlotView;
import java.time.Instant;
import java.util.List;

/** Adaptador del lado de lectura de Agenda: mismo almacén, modelo separado (DTO). */
public final class JpaSlotReadModel implements SlotReadModel {

    private final SlotQueryRepository queries;

    public JpaSlotReadModel(SlotQueryRepository queries) {
        this.queries = queries;
    }

    @Override
    public List<AvailableSlotView> availableFor(String specialistId, Instant from) {
        return queries.findAvailable(specialistId, from);
    }
}
