package com.redsaludandina.scheduling.application.query;

import java.time.Instant;

/** Modelo de lectura: un cupo que todavía se puede reservar. */
public record AvailableSlotView(String slotId, String specialistId, Instant start, Instant end) {
}
