package com.redsaludandina.legacy.application.query;

import com.redsaludandina.legacy.application.port.HistoryReadModelReader;
import com.redsaludandina.shared.application.ResourceNotFoundException;
import com.redsaludandina.shared.application.cqrs.QueryHandler;
import com.redsaludandina.shared.domain.Require;
import java.time.Clock;

public final class GetClinicalHistoryHandler implements QueryHandler<GetClinicalHistoryQuery, ClinicalHistoryView> {

    private final HistoryReadModelReader readModel;
    private final Clock clock;

    public GetClinicalHistoryHandler(HistoryReadModelReader readModel, Clock clock) {
        this.readModel = Require.notNull(readModel, "readModel");
        this.clock = Require.notNull(clock, "clock");
    }

    @Override
    public ClinicalHistoryView handle(GetClinicalHistoryQuery query) {
        String patientId = Require.text(query.patientId(), "patientId");
        return readModel.find(patientId)
                .map(view -> view.evaluatedAt(clock.instant()))
                .orElseThrow(() -> new ResourceNotFoundException("el historial de " + patientId));
    }
}
