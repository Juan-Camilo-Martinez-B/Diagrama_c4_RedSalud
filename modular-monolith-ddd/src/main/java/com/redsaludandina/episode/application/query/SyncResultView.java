package com.redsaludandina.episode.application.query;

import java.util.List;

/**
 * Modelo de lectura de la respuesta R de un lote. {@code replayed} indica si el último envío se
 * contestó repitiendo R (HTTP 200) o procesando el lote (HTTP 201).
 */
public record SyncResultView(List<SyncItemView> items, boolean replayed) {

    public record SyncItemView(String changeId, String status, String code, String message) {
    }
}
