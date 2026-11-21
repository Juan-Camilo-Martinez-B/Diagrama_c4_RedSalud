package com.redsaludandina.scheduling.domain.model;

import com.redsaludandina.shared.domain.ClinicalId;
import com.redsaludandina.shared.domain.Require;
import java.time.Instant;

/** Reserva de un cupo por un paciente desde un centro (entidad interna del agregado Slot). */
public record Booking(ClinicalId id, String patientId, String centerId, Instant bookedAt) {

    public Booking {
        Require.notNull(id, "bookingId");
        patientId = Require.text(patientId, "patientId");
        centerId = Require.text(centerId, "centerId");
        Require.notNull(bookedAt, "bookedAt");
    }
}
