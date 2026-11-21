package com.redsaludandina.referral.domain.model;

import java.util.Optional;

/**
 * Ciclo de vida de una interconsulta: solicitada → agendada → atendida → cerrada, sin saltos ni
 * retrocesos. Cada estado sabe qué alerta queda abierta mientras se espera el siguiente.
 */
public enum ReferralStatus {

    REQUESTED(AlertType.PENDING_SCHEDULING),
    SCHEDULED(AlertType.PENDING_ATTENTION),
    ATTENDED(AlertType.PENDING_CLOSURE),
    CLOSED(null);

    private final AlertType pendingAlert;

    ReferralStatus(AlertType pendingAlert) {
        this.pendingAlert = pendingAlert;
    }

    public Optional<AlertType> pendingAlert() {
        return Optional.ofNullable(pendingAlert);
    }

    public boolean canAdvanceTo(ReferralStatus target) {
        return target.ordinal() == ordinal() + 1;
    }
}
