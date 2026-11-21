package com.redsaludandina.shared.infrastructure.persistence.outbox;

import com.redsaludandina.shared.application.messaging.OutboxMessage;
import com.redsaludandina.shared.application.messaging.OutboxPort;
import com.redsaludandina.shared.infrastructure.messaging.EventSerializer;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.data.domain.Limit;

/** Outbox de un contexto sobre Spring Data JPA. */
public final class JpaOutbox<T extends OutboxRecordEntity> implements OutboxPort {

    private final OutboxRecordRepository<T> repository;
    private final Supplier<T> newRecord;
    private final EventSerializer serializer;

    public JpaOutbox(OutboxRecordRepository<T> repository, Supplier<T> newRecord, EventSerializer serializer) {
        this.repository = repository;
        this.newRecord = newRecord;
        this.serializer = serializer;
    }

    @Override
    public void append(List<OutboxMessage> messages) {
        messages.forEach(this::appendOne);
    }

    private void appendOne(OutboxMessage message) {
        T outboxRecord = newRecord.get();
        outboxRecord.fill(message.eventId(), message.context(), message.aggregateId(),
                serializer.type(message.event()), serializer.toJson(message.event()), message.recordedAt());
        repository.save(outboxRecord);
    }

    @Override
    public List<OutboxMessage> pending(int limit) {
        return repository.findByPublishedAtIsNullOrderBySequenceAsc(Limit.of(limit)).stream()
                .map(this::toMessage)
                .toList();
    }

    private OutboxMessage toMessage(T outboxRecord) {
        return new OutboxMessage(outboxRecord.eventId(), outboxRecord.context(), outboxRecord.aggregateId(),
                serializer.fromJson(outboxRecord.type(), outboxRecord.payload()), outboxRecord.recordedAt());
    }

    @Override
    public void markPublished(UUID eventId, Instant publishedAt) {
        repository.markPublished(eventId, publishedAt);
    }
}
