package com.redsaludandina.scheduling.application.port;

import com.redsaludandina.scheduling.application.query.AvailableSlotView;
import java.time.Instant;
import java.util.List;

/** Lado de lectura de Agenda: devuelve DTO de lectura, nunca el agregado (puerto de salida). */
public interface SlotReadModel {

    List<AvailableSlotView> availableFor(String specialistId, Instant from);
}
