package com.redsaludandina.episode.application.command.sync;

import static com.redsaludandina.TestIds.ulid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.redsaludandina.episode.application.port.SyncReceiptRepository;
import com.redsaludandina.episode.domain.sync.IdempotencyKeyReusedException;
import com.redsaludandina.episode.domain.sync.ItemResult;
import com.redsaludandina.episode.domain.sync.ItemStatus;
import com.redsaludandina.episode.domain.sync.SyncReceipt;
import com.redsaludandina.shared.application.DirectUnitOfWork;
import com.redsaludandina.shared.application.DuplicateRecordException;
import com.redsaludandina.shared.application.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Secuencia de idempotencia por lote del caso, sin base de datos. */
class SyncOfflineBatchHandlerTest {

    private static final Instant STAMP = Instant.parse("2026-03-01T08:00:00Z");

    private final Map<String, SyncReceipt> stored = new HashMap<>();
    private final List<String> applied = new ArrayList<>();
    private RuntimeException closeFailure;
    private SyncOfflineBatchHandler handler;

    @BeforeEach
    void setUp() {
        SyncReceiptRepository receipts = new SyncReceiptRepository() {
            @Override
            public Optional<SyncReceipt> find(String key) {
                return Optional.ofNullable(stored.get(key));
            }

            @Override
            public void save(SyncReceipt receipt) {
                stored.put(receipt.idempotencyKey(), receipt);
            }
        };
        OfflineChangeTarget target = new OfflineChangeTarget(
                command -> applied.add("open"),
                command -> applied.add("care:" + command.field()),
                command -> applied.add("attach"),
                command -> close());
        handler = new SyncOfflineBatchHandler(receipts, target, new DirectUnitOfWork(),
                Clock.fixed(STAMP, ZoneOffset.UTC));
    }

    private void close() {
        if (closeFailure != null) {
            throw closeFailure;
        }
        applied.add("close");
    }

    private static List<OfflineItem> batch() {
        String episode = ulid(1);
        return List.of(
                new OfflineItem("c3", episode, 3, new OfflineChange.CloseTeleconsultation(episode)),
                new OfflineItem("c1", episode, 1, new OfflineChange.OpenEpisode(episode, "pac-1", null)),
                new OfflineItem("c2", episode, 2, new OfflineChange.RecordCare(episode, "REASON", "Fiebre", STAMP)),
                new OfflineItem("c4", ulid(2), 1, new OfflineChange.BookSlot(ulid(2), ulid(3), "pac-1")));
    }

    private List<ItemStatus> statuses(String key) {
        return stored.get(key).result().items().stream().map(ItemResult::status).toList();
    }

    @Test
    void appliesInAggregateOrderAndRejectsSlotsAtTheEdge() {
        handler.handle(new SyncOfflineBatchCommand("k-1", "centro-a", batch()));

        assertThat(applied).containsExactly("open", "care:REASON", "close");
        assertThat(statuses("k-1")).containsExactly(ItemStatus.APPLIED, ItemStatus.APPLIED, ItemStatus.APPLIED,
                ItemStatus.REJECTED);
        assertThat(stored.get("k-1").result().items().get(3).code()).isEqualTo(ItemResult.REQUIRES_SERVER);
        assertThat(stored.get("k-1").replayed()).isFalse();
    }

    @Test
    void theSameBatchIsReplayedWithoutApplyingAnything() {
        handler.handle(new SyncOfflineBatchCommand("k-1", "centro-a", batch()));
        applied.clear();

        handler.handle(new SyncOfflineBatchCommand("k-1", "centro-a", batch()));

        assertThat(applied).isEmpty();
        assertThat(stored.get("k-1").replayed()).isTrue();
    }

    @Test
    void theSameKeyWithAnotherBatchIsRejected() {
        handler.handle(new SyncOfflineBatchCommand("k-1", "centro-a", batch()));

        assertThatThrownBy(() -> handler.handle(new SyncOfflineBatchCommand("k-1", "centro-a", batch().subList(0, 1))))
                .isInstanceOf(IdempotencyKeyReusedException.class);
    }

    @Test
    void aRaceWithAnotherDeliveryCountsAsAlreadyApplied() {
        closeFailure = new DuplicateRecordException("ya existe", null);

        handler.handle(new SyncOfflineBatchCommand("k-1", "centro-a", batch()));

        assertThat(statuses("k-1").get(0)).isEqualTo(ItemStatus.ALREADY_APPLIED);
    }

    @Test
    void aPendingChangeIsProcessedAgainOnTheNextDelivery() {
        closeFailure = new ResourceNotFoundException("el episodio");
        handler.handle(new SyncOfflineBatchCommand("k-1", "centro-a", batch()));
        assertThat(statuses("k-1").get(0)).isEqualTo(ItemStatus.PENDING);

        closeFailure = null;
        handler.handle(new SyncOfflineBatchCommand("k-1", "centro-a", batch()));

        assertThat(statuses("k-1").get(0)).isEqualTo(ItemStatus.APPLIED);
        assertThat(stored.get("k-1").replayed()).isFalse();
    }
}
