package com.redsaludandina.scheduling.infrastructure.persistence;

import com.redsaludandina.shared.infrastructure.persistence.outbox.OutboxRecordRepository;

public interface SchedulingOutboxRepository extends OutboxRecordRepository<SchedulingOutboxRecord> {
}
