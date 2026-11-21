package com.redsaludandina.identity.infrastructure.persistence;

import com.redsaludandina.shared.infrastructure.persistence.outbox.OutboxRecordRepository;

public interface IdentityOutboxRepository extends OutboxRecordRepository<IdentityOutboxRecord> {
}
