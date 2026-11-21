package com.redsaludandina.insights.application.query;

import com.redsaludandina.insights.application.port.UsageIndicatorReadModel;
import com.redsaludandina.shared.application.ResourceNotFoundException;
import com.redsaludandina.shared.application.cqrs.QueryHandler;
import com.redsaludandina.shared.domain.Require;
import java.time.YearMonth;

public final class GetUsageIndicatorHandler implements QueryHandler<GetUsageIndicatorQuery, UsageIndicatorView> {

    private final UsageIndicatorReadModel readModel;

    public GetUsageIndicatorHandler(UsageIndicatorReadModel readModel) {
        this.readModel = Require.notNull(readModel, "readModel");
    }

    @Override
    public UsageIndicatorView handle(GetUsageIndicatorQuery query) {
        String centerId = Require.text(query.centerId(), "centerId");
        String period = YearMonth.parse(Require.text(query.period(), "period")).toString();
        return readModel.find(centerId, period)
                .orElseThrow(() -> new ResourceNotFoundException("indicador de " + centerId + " en " + period));
    }
}
