package com.redsaludandina.scheduling.domain.model;

import com.redsaludandina.shared.domain.Require;
import java.time.Instant;

/** Franja horaria de un cupo. */
public record TimeWindow(Instant start, Instant end) {

    public TimeWindow {
        Require.notNull(start, "start");
        Require.notNull(end, "end");
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("La franja debe terminar después de empezar");
        }
    }

    public boolean hasStarted(Instant now) {
        return !now.isBefore(start);
    }
}
