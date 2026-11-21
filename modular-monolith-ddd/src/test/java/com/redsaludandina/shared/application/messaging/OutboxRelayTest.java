package com.redsaludandina.shared.application.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.redsaludandina.shared.application.messaging.OutboxRelay.RelayResult;
import com.redsaludandina.shared.event.ReferralClosed;
import com.redsaludandina.TestIds;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OutboxRelayTest {

    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");

    private static OutboxMessage message(String aggregateId) {
        return new OutboxMessage(UUID.randomUUID(), "referral", aggregateId,
                new ReferralClosed(TestIds.id(1), "centro-a", NOW), NOW);
    }

    @Test
    void aFailureDefersTheFollowingMessagesOfTheSameAggregateOnly() {
        OutboxMessage failing = message("A");
        List<OutboxMessage> pending = List.of(failing, message("A"), message("B"));
        Set<UUID> published = new HashSet<>();
        OutboxPort outbox = new OutboxPort() {
            @Override
            public void append(List<OutboxMessage> messages) {
            }

            @Override
            public List<OutboxMessage> pending(int limit) {
                return pending;
            }

            @Override
            public void markPublished(UUID eventId, Instant publishedAt) {
                published.add(eventId);
            }
        };
        List<String> delivered = new ArrayList<>();
        EventBusPort bus = sent -> {
            if (sent == failing) {
                throw new IllegalStateException("bus caído");
            }
            delivered.add(sent.aggregateId());
        };

        RelayResult result = new OutboxRelay(outbox, bus, Clock.fixed(NOW, ZoneOffset.UTC)).relay(10);

        assertThat(result).isEqualTo(new RelayResult(1, 1, 1));
        assertThat(delivered).containsExactly("B");
        assertThat(published).hasSize(1);
    }
}
