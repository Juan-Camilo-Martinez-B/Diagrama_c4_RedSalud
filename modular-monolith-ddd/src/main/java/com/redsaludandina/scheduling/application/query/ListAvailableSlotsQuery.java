package com.redsaludandina.scheduling.application.query;

import com.redsaludandina.shared.application.cqrs.Query;
import java.util.List;

/** ¿Qué cupos futuros y libres tiene este especialista? */
public record ListAvailableSlotsQuery(String specialistId) implements Query<List<AvailableSlotView>> {
}
