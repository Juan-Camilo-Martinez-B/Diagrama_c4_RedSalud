package com.redsaludandina.prescription.infrastructure.persistence;

import com.redsaludandina.shared.infrastructure.persistence.outbox.OutboxRecordRepository;

public interface PrescriptionOutboxRepository extends OutboxRecordRepository<PrescriptionOutboxRecord> {
}
