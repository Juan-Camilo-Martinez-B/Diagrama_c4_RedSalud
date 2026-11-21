package com.redsaludandina.episode.application.port;

import com.redsaludandina.episode.application.query.SyncResultView;
import java.util.Optional;

/** Lado de lectura de los recibos de sincronización (puerto de salida). */
public interface SyncReceiptReadModel {

    Optional<SyncResultView> find(String idempotencyKey);
}
