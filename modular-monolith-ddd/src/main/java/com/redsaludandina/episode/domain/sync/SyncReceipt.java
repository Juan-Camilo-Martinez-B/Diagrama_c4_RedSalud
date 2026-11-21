package com.redsaludandina.episode.domain.sync;

import com.redsaludandina.shared.domain.Require;
import java.time.Instant;

/**
 * Recibo de idempotencia de un lote (Caso §4, diagrama de secuencia): la Idempotency-Key, la huella
 * SHA-256 del lote y la respuesta R. Reglas:
 *
 * <ul>
 *   <li>Misma clave y mismo lote con R definitiva: se repite R sin volver a aplicar nada.</li>
 *   <li>Misma clave con otro lote: se rechaza ({@link IdempotencyKeyReusedException}).</li>
 *   <li>Si R tenía pendientes, el reintento vuelve a procesar el lote (los cambios son idempotentes).</li>
 * </ul>
 */
public record SyncReceipt(String idempotencyKey, String batchHash, SyncResult result, boolean replayed,
                          Instant recordedAt) {

    public static final int MAX_KEY_LENGTH = 100;

    public SyncReceipt {
        idempotencyKey = validKey(idempotencyKey);
        batchHash = Require.text(batchHash, "batchHash");
        Require.notNull(result, "result");
        Require.notNull(recordedAt, "recordedAt");
    }

    public static SyncReceipt first(String idempotencyKey, String batchHash, SyncResult result, Instant now) {
        return new SyncReceipt(idempotencyKey, batchHash, result, false, now);
    }

    public static String validKey(String idempotencyKey) {
        String key = Require.text(idempotencyKey, "Idempotency-Key");
        if (key.length() > MAX_KEY_LENGTH) {
            throw new IllegalArgumentException("La Idempotency-Key no puede superar " + MAX_KEY_LENGTH + " caracteres");
        }
        return key;
    }

    public void requireSameBatch(String otherBatchHash) {
        if (!batchHash.equals(otherBatchHash)) {
            throw new IdempotencyKeyReusedException(idempotencyKey);
        }
    }

    /** ¿Se puede contestar el reintento repitiendo R, sin aplicar nada? */
    public boolean canReplay() {
        return result.definitive();
    }

    public SyncReceipt asReplay() {
        return new SyncReceipt(idempotencyKey, batchHash, result, true, recordedAt);
    }
}
