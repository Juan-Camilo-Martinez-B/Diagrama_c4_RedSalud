package com.redsaludandina.shared.event;

import com.redsaludandina.shared.domain.ClinicalId;
import com.redsaludandina.shared.domain.DomainEvent;
import com.redsaludandina.shared.domain.Require;
import java.time.Instant;

/**
 * Agenda reservó un cupo. Interconsulta lo usa para pasar a «agendada» y la analítica para medir el
 * tiempo de espera ({@code slotStart - occurredAt}).
 */
public record SlotBooked(
        ClinicalId slotId,
        ClinicalId bookingId,
        String patientId,
        String centerId,
        String specialistId,
        Instant slotStart,
        Instant occurredAt) implements DomainEvent {

    public SlotBooked {
        Require.notNull(slotId, "slotId");
        Require.notNull(bookingId, "bookingId");
        patientId = Require.text(patientId, "patientId");
        centerId = Require.text(centerId, "centerId");
        specialistId = Require.text(specialistId, "specialistId");
        Require.notNull(slotStart, "slotStart");
        Require.notNull(occurredAt, "occurredAt");
    }
}
