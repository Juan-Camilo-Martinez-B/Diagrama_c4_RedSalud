package com.redsaludandina.referral.infrastructure.persistence;

import com.redsaludandina.shared.infrastructure.persistence.outbox.OutboxRecordRepository;

public interface ReferralOutboxRepository extends OutboxRecordRepository<ReferralOutboxRecord> {
}
