package com.redsaludandina.shared.event;

import com.redsaludandina.shared.domain.ClinicalId;
import com.redsaludandina.shared.domain.DomainEvent;
import com.redsaludandina.shared.domain.Require;
import java.time.Instant;

/** Una interconsulta llegó a «cerrada». La analítica lo usa para la tasa de cierre. */
public record ReferralClosed(
        ClinicalId referralId,
        String centerId,
        Instant occurredAt) implements DomainEvent {

    public ReferralClosed {
        Require.notNull(referralId, "referralId");
        centerId = Require.text(centerId, "centerId");
        Require.notNull(occurredAt, "occurredAt");
    }
}
