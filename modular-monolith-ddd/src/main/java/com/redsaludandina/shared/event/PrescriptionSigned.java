package com.redsaludandina.shared.event;

import com.redsaludandina.shared.domain.ClinicalId;
import com.redsaludandina.shared.domain.DomainEvent;
import com.redsaludandina.shared.domain.Require;
import java.time.Instant;

/** Una receta quedó firmada e inmutable; a partir de aquí se envía a la farmacia del centro. */
public record PrescriptionSigned(
        ClinicalId prescriptionId,
        ClinicalId episodeId,
        String signerId,
        Instant occurredAt) implements DomainEvent {

    public PrescriptionSigned {
        Require.notNull(prescriptionId, "prescriptionId");
        Require.notNull(episodeId, "episodeId");
        signerId = Require.text(signerId, "signerId");
        Require.notNull(occurredAt, "occurredAt");
    }
}
