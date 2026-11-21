package com.redsaludandina.legacy.infrastructure.persistence;

import com.redsaludandina.shared.infrastructure.persistence.outbox.OutboxRecordRepository;

public interface LegacyOutboxRepository extends OutboxRecordRepository<LegacyOutboxRecord> {
}
