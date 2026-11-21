package com.redsaludandina.episode.application.command.sync;

import com.redsaludandina.episode.application.port.SyncReceiptRepository;
import com.redsaludandina.episode.domain.sync.SyncReceipt;
import com.redsaludandina.episode.domain.sync.SyncResult;
import com.redsaludandina.shared.application.DuplicateRecordException;
import com.redsaludandina.shared.application.UnitOfWork;
import com.redsaludandina.shared.application.cqrs.CommandHandler;
import com.redsaludandina.shared.domain.Require;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

/**
 * Sincronización idempotente por lote (diagrama de secuencia del caso):
 *
 * <ol>
 *   <li>Busca el recibo de la Idempotency-Key. Si existe con otro lote, rechaza (422).</li>
 *   <li>Si existe con R definitiva, solo marca el envío como repetición: no aplica nada.</li>
 *   <li>Si no, aplica los cambios y guarda el recibo con la huella del lote y R.</li>
 * </ol>
 */
public final class SyncOfflineBatchHandler implements CommandHandler<SyncOfflineBatchCommand> {

    private final SyncReceiptRepository receipts;
    private final SyncBatchProcessor processor;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public SyncOfflineBatchHandler(SyncReceiptRepository receipts, OfflineChangeTarget target, UnitOfWork unitOfWork,
                                   Clock clock) {
        this.receipts = Require.notNull(receipts, "receipts");
        this.processor = new SyncBatchProcessor(target);
        this.unitOfWork = Require.notNull(unitOfWork, "unitOfWork");
        this.clock = Require.notNull(clock, "clock");
    }

    @Override
    public void handle(SyncOfflineBatchCommand command) {
        String key = SyncReceipt.validKey(command.idempotencyKey());
        String centerId = Require.text(command.centerId(), "centerId");
        List<OfflineItem> items = Require.notNull(command.items(), "items");
        String batchHash = BatchFingerprint.of(centerId, items);
        Optional<SyncReceipt> previous = unitOfWork.call(() -> receipts.find(key));
        previous.ifPresent(receipt -> receipt.requireSameBatch(batchHash));
        if (previous.filter(SyncReceipt::canReplay).isPresent()) {
            save(previous.get().asReplay());
            return;
        }
        SyncResult result = processor.process(centerId, items);
        save(SyncReceipt.first(key, batchHash, result, clock.instant()));
    }

    /** Si otro envío simultáneo con la misma clave guardó primero, su recibo es igual de válido. */
    private void save(SyncReceipt receipt) {
        try {
            unitOfWork.run(() -> receipts.save(receipt));
        } catch (DuplicateRecordException concurrentDelivery) {
            return;
        }
    }
}
