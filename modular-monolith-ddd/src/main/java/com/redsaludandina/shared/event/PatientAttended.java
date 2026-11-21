package com.redsaludandina.shared.event;

import com.redsaludandina.shared.domain.ClinicalId;
import com.redsaludandina.shared.domain.DomainEvent;
import com.redsaludandina.shared.domain.Require;
import java.time.Instant;

/**
 * Episodio clínico terminó la teleconsulta. Interconsulta lo usa para pasar a «atendida» y la
 * analítica para contar teleconsultas por centro. {@code slotId} es nulo si la atención no vino de un
 * cupo de la agenda.
 */
public record PatientAttended(
        ClinicalId episodeId,
        String patientId,
        ClinicalId slotId,
        String centerId,
        Instant occurredAt) implements DomainEvent {

    public PatientAttended {
        Require.notNull(episodeId, "episodeId");
        patientId = Require.text(patientId, "patientId");
        centerId = Require.text(centerId, "centerId");
        Require.notNull(occurredAt, "occurredAt");
    }
}
