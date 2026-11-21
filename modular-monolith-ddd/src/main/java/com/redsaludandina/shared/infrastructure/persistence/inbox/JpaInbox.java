package com.redsaludandina.shared.infrastructure.persistence.inbox;

import com.redsaludandina.shared.application.messaging.InboxPort;
import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Inbox de un contexto sobre Spring Data JPA. Si dos entregas del mismo evento compiten, la clave
 * primaria rechaza la segunda al confirmar y el evento se reintenta (y entonces ya figura como visto).
 */
public final class JpaInbox<T extends InboxRecordEntity> implements InboxPort {

    private final InboxRecordRepository<T> repository;
    private final Supplier<T> newRecord;
    private final Clock clock;

    public JpaInbox(InboxRecordRepository<T> repository, Supplier<T> newRecord, Clock clock) {
        this.repository = repository;
        this.newRecord = newRecord;
        this.clock = clock;
    }

    @Override
    public boolean registerIfNew(String consumer, UUID eventId) {
        if (repository.existsById(InboxRecordEntity.idOf(consumer, eventId))) {
            return false;
        }
        T inboxRecord = newRecord.get();
        inboxRecord.fill(consumer, eventId, clock.instant());
        repository.save(inboxRecord);
        return true;
    }
}
