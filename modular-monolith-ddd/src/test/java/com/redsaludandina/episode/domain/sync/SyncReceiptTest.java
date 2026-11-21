package com.redsaludandina.episode.domain.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class SyncReceiptTest {

    private static final Instant NOW = Instant.parse("2026-03-01T12:00:00Z");

    @Test
    void aDefinitiveResultCanBeReplayed() {
        SyncReceipt receipt = SyncReceipt.first("k-1", "hash", new SyncResult(List.of(ItemResult.applied("c1"))), NOW);

        assertThat(receipt.canReplay()).isTrue();
        assertThat(receipt.asReplay().replayed()).isTrue();
    }

    @Test
    void aResultWithPendingItemsIsProcessedAgain() {
        SyncResult pending = new SyncResult(List.of(ItemResult.pending("c1", ItemResult.UNAVAILABLE, "reintente")));

        assertThat(SyncReceipt.first("k-1", "hash", pending, NOW).canReplay()).isFalse();
    }

    @Test
    void reusingTheKeyWithAnotherBatchIsRejected() {
        SyncReceipt receipt = SyncReceipt.first("k-1", "hash", new SyncResult(List.of()), NOW);

        assertThatThrownBy(() -> receipt.requireSameBatch("other-hash"))
                .isInstanceOf(IdempotencyKeyReusedException.class);
    }

    @Test
    void theKeyIsBounded() {
        assertThatThrownBy(() -> SyncReceipt.validKey("x".repeat(SyncReceipt.MAX_KEY_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
