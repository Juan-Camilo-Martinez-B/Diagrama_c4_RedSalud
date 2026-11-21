package com.redsaludandina.scheduling.infrastructure.persistence;

import com.redsaludandina.shared.infrastructure.persistence.outbox.OutboxRecordEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Outbox del contexto Agenda, en su propio esquema. */
@Entity(name = "SchedulingOutboxRecord")
@Table(schema = "scheduling", name = "outbox")
public class SchedulingOutboxRecord extends OutboxRecordEntity {
}
