package com.redsaludandina.legacy.infrastructure.persistence;

import com.redsaludandina.shared.infrastructure.persistence.outbox.OutboxRecordEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Outbox del contexto Integración con el legado (lecturas del historial), en su propio esquema. */
@Entity(name = "LegacyOutboxRecord")
@Table(schema = "legacy", name = "outbox")
public class LegacyOutboxRecord extends OutboxRecordEntity {
}
