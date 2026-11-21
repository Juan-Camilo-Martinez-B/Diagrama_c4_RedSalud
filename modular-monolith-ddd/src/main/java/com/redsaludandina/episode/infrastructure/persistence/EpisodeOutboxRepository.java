package com.redsaludandina.episode.infrastructure.persistence;

import com.redsaludandina.shared.infrastructure.persistence.outbox.OutboxRecordRepository;

public interface EpisodeOutboxRepository extends OutboxRecordRepository<EpisodeOutboxRecord> {
}
