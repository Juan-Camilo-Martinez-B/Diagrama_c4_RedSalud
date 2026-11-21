package com.redsaludandina.episode.infrastructure.persistence;

import com.redsaludandina.episode.application.port.SyncReceiptReadModel;
import com.redsaludandina.episode.application.query.SyncResultView;
import com.redsaludandina.episode.application.query.SyncResultView.SyncItemView;
import com.redsaludandina.episode.infrastructure.persistence.SyncReceiptQueryRepository.SyncReceiptRow;
import java.util.List;
import java.util.Optional;
import tools.jackson.databind.json.JsonMapper;

/** Adaptador del lado de lectura de los recibos: JSON guardado → DTO de lectura. */
public final class JpaSyncReceiptReadModel implements SyncReceiptReadModel {

    private final SyncReceiptQueryRepository queries;
    private final JsonMapper json;

    public JpaSyncReceiptReadModel(SyncReceiptQueryRepository queries, JsonMapper json) {
        this.queries = queries;
        this.json = json;
    }

    @Override
    public Optional<SyncResultView> find(String idempotencyKey) {
        return queries.findByIdempotencyKey(idempotencyKey).map(this::toView);
    }

    private SyncResultView toView(SyncReceiptRow row) {
        return new SyncResultView(List.of(json.readValue(row.getResult(), SyncItemView[].class)), row.isReplayed());
    }
}
