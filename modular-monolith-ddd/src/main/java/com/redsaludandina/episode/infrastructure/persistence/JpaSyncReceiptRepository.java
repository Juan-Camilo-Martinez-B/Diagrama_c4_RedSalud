package com.redsaludandina.episode.infrastructure.persistence;

import com.redsaludandina.episode.application.port.SyncReceiptRepository;
import com.redsaludandina.episode.domain.sync.ItemResult;
import com.redsaludandina.episode.domain.sync.ItemStatus;
import com.redsaludandina.episode.domain.sync.SyncReceipt;
import com.redsaludandina.episode.domain.sync.SyncResult;
import java.util.Arrays;
import java.util.Optional;
import tools.jackson.databind.json.JsonMapper;

/** Adaptador del lado de escritura de los recibos: R se guarda como JSON. */
public final class JpaSyncReceiptRepository implements SyncReceiptRepository {

    /** Forma JSON de cada resultado; el modelo de lectura la lee con los mismos nombres. */
    record StoredItem(String changeId, String status, String code, String message) {
    }

    private final SyncReceiptJpaRepository jpa;
    private final JsonMapper json;

    public JpaSyncReceiptRepository(SyncReceiptJpaRepository jpa, JsonMapper json) {
        this.jpa = jpa;
        this.json = json;
    }

    @Override
    public Optional<SyncReceipt> find(String idempotencyKey) {
        return jpa.findById(idempotencyKey).map(this::toDomain);
    }

    @Override
    public void save(SyncReceipt receipt) {
        StoredItem[] items = receipt.result().items().stream()
                .map(item -> new StoredItem(item.changeId(), item.status().name(), item.code(), item.message()))
                .toArray(StoredItem[]::new);
        jpa.save(new SyncReceiptEntity(receipt.idempotencyKey(), receipt.batchHash(), json.writeValueAsString(items),
                receipt.result().definitive(), receipt.replayed(), receipt.recordedAt()));
    }

    private SyncReceipt toDomain(SyncReceiptEntity entity) {
        SyncResult result = new SyncResult(Arrays.stream(json.readValue(entity.result(), StoredItem[].class))
                .map(item -> new ItemResult(item.changeId(), ItemStatus.valueOf(item.status()), item.code(), item.message()))
                .toList());
        return new SyncReceipt(entity.idempotencyKey(), entity.batchHash(), result, entity.replayed(), entity.recordedAt());
    }
}
