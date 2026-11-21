package com.redsaludandina.episode.infrastructure.persistence;

import com.redsaludandina.shared.infrastructure.persistence.outbox.OutboxRecordEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Outbox del contexto Episodio clínico, en su propio esquema. */
@Entity(name = "EpisodeOutboxRecord")
@Table(schema = "episode", name = "outbox")
public class EpisodeOutboxRecord extends OutboxRecordEntity {
}
