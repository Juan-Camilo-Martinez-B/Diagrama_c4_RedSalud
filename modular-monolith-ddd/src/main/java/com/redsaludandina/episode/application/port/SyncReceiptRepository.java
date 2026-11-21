package com.redsaludandina.episode.application.port;

import com.redsaludandina.episode.domain.sync.SyncReceipt;
import java.util.Optional;

/** Lado de escritura de los recibos de idempotencia por lote (puerto de salida). */
public interface SyncReceiptRepository {

    Optional<SyncReceipt> find(String idempotencyKey);

    void save(SyncReceipt receipt);
}
