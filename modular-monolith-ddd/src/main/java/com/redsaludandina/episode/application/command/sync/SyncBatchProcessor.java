package com.redsaludandina.episode.application.command.sync;

import com.redsaludandina.episode.domain.sync.ItemResult;
import com.redsaludandina.episode.domain.sync.SyncResult;
import com.redsaludandina.shared.domain.Require;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Aplica los cambios de un lote en orden por agregado y secuencia. Si un cambio queda pendiente, los
 * siguientes del MISMO agregado también quedan pendientes (no se aplica «cerrar» sin «abrir»); los de
 * otros agregados siguen. Cada cambio se aplica en su propia transacción.
 */
final class SyncBatchProcessor {

    private final OfflineChangeTarget target;

    SyncBatchProcessor(OfflineChangeTarget target) {
        this.target = Require.notNull(target, "target");
    }

    SyncResult process(String centerId, List<OfflineItem> items) {
        ItemResult[] results = new ItemResult[items.size()];
        Set<String> stoppedAggregates = new HashSet<>();
        for (int index : inAggregateOrder(items)) {
            results[index] = processInOrder(centerId, items.get(index), stoppedAggregates);
        }
        return new SyncResult(List.of(results));
    }

    private static List<Integer> inAggregateOrder(List<OfflineItem> items) {
        return IntStream.range(0, items.size()).boxed()
                .sorted(Comparator.comparing((Integer index) -> String.valueOf(items.get(index).aggregateId()))
                        .thenComparingLong(index -> items.get(index).sequence()))
                .toList();
    }

    private ItemResult processInOrder(String centerId, OfflineItem item, Set<String> stoppedAggregates) {
        ItemResult result = stoppedAggregates.contains(item.aggregateId())
                ? ItemResult.pending(item.changeId(), ItemResult.ORDER_PENDING,
                        "Espera a que se aplique un cambio anterior del mismo agregado")
                : apply(centerId, item);
        if (result.isPending()) {
            stoppedAggregates.add(item.aggregateId());
        }
        return result;
    }

    private ItemResult apply(String centerId, OfflineItem item) {
        try {
            Require.notNull(item.change(), "change").applyTo(target, centerId);
            return ItemResult.applied(item.changeId());
        } catch (RuntimeException failure) {
            return FailureClassifier.classify(item.changeId(), failure);
        }
    }
}
