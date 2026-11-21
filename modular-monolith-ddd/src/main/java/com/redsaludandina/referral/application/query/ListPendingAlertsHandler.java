package com.redsaludandina.referral.application.query;

import com.redsaludandina.referral.application.port.ReferralReadModel;
import com.redsaludandina.shared.application.cqrs.QueryHandler;
import com.redsaludandina.shared.domain.Require;
import java.util.List;

public final class ListPendingAlertsHandler implements QueryHandler<ListPendingAlertsQuery, List<PendingReferralView>> {

    private final ReferralReadModel readModel;

    public ListPendingAlertsHandler(ReferralReadModel readModel) {
        this.readModel = Require.notNull(readModel, "readModel");
    }

    @Override
    public List<PendingReferralView> handle(ListPendingAlertsQuery query) {
        return readModel.withPendingAlerts(Require.text(query.centerId(), "centerId"));
    }
}
