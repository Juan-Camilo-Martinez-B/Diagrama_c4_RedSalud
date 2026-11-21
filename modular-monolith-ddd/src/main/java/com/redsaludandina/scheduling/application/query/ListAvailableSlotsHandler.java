package com.redsaludandina.scheduling.application.query;

import com.redsaludandina.scheduling.application.port.SlotReadModel;
import com.redsaludandina.shared.application.cqrs.QueryHandler;
import com.redsaludandina.shared.domain.Require;
import java.time.Clock;
import java.util.List;

/** Lista los cupos libres desde ahora, leyendo del modelo de lectura. */
public final class ListAvailableSlotsHandler implements QueryHandler<ListAvailableSlotsQuery, List<AvailableSlotView>> {

    private final SlotReadModel readModel;
    private final Clock clock;

    public ListAvailableSlotsHandler(SlotReadModel readModel, Clock clock) {
        this.readModel = Require.notNull(readModel, "readModel");
        this.clock = Require.notNull(clock, "clock");
    }

    @Override
    public List<AvailableSlotView> handle(ListAvailableSlotsQuery query) {
        return readModel.availableFor(Require.text(query.specialistId(), "specialistId"), clock.instant());
    }
}
