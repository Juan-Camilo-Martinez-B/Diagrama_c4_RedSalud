package com.redsaludandina.episode.application.command.sync;

import com.redsaludandina.shared.application.cqrs.Command;
import java.util.List;

/**
 * Sincroniza un lote de cambios offline de un centro (POST /episodes/sync). La Idempotency-Key
 * identifica el lote: reenviarlo no vuelve a aplicar nada. La respuesta R se consulta después con
 * {@code GetSyncResultQuery} (el comando no devuelve datos).
 */
public record SyncOfflineBatchCommand(String idempotencyKey, String centerId, List<OfflineItem> items)
        implements Command {
}
