package com.redsaludandina.shared.event;

import com.redsaludandina.shared.domain.ClinicalId;
import com.redsaludandina.shared.domain.DomainEvent;
import com.redsaludandina.shared.domain.Require;
import java.time.Instant;

/** Un médico rural pidió una interconsulta. La analítica lo usa como base de la tasa de cierre. */
public record ReferralRequested(
        ClinicalId referralId,
        String centerId,
        String specialty,
        Instant occurredAt) implements DomainEvent {

    public ReferralRequested {
        Require.notNull(referralId, "referralId");
        centerId = Require.text(centerId, "centerId");
        specialty = Require.text(specialty, "specialty");
        Require.notNull(occurredAt, "occurredAt");
    }
}
