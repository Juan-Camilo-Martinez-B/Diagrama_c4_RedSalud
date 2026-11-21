package com.redsaludandina.referral.infrastructure.persistence;

import com.redsaludandina.referral.application.port.ReferralReadModel;
import com.redsaludandina.referral.application.query.PendingReferralView;
import java.util.List;

/** Adaptador del lado de lectura de Interconsulta: filas → DTO, sin pasar por el dominio. */
public final class JpaReferralReadModel implements ReferralReadModel {

    private final ReferralQueryRepository queries;

    public JpaReferralReadModel(ReferralQueryRepository queries) {
        this.queries = queries;
    }

    @Override
    public List<PendingReferralView> withPendingAlerts(String centerId) {
        return queries.findWithPendingAlerts(centerId).stream().map(ReferralEntity::toPendingView).toList();
    }
}
