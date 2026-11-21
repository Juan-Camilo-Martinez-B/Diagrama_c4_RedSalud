package com.redsaludandina.episode.application.query;

import com.redsaludandina.episode.application.port.SyncReceiptReadModel;
import com.redsaludandina.shared.application.ResourceNotFoundException;
import com.redsaludandina.shared.application.cqrs.QueryHandler;
import com.redsaludandina.shared.domain.Require;

public final class GetSyncResultHandler implements QueryHandler<GetSyncResultQuery, SyncResultView> {

    private final SyncReceiptReadModel readModel;

    public GetSyncResultHandler(SyncReceiptReadModel readModel) {
        this.readModel = Require.notNull(readModel, "readModel");
    }

    @Override
    public SyncResultView handle(GetSyncResultQuery query) {
        String key = Require.text(query.idempotencyKey(), "Idempotency-Key");
        return readModel.find(key).orElseThrow(() -> new ResourceNotFoundException("el lote " + key));
    }
}
