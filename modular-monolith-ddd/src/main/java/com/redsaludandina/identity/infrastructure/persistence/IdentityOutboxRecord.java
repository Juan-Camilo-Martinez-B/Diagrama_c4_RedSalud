package com.redsaludandina.identity.infrastructure.persistence;

import com.redsaludandina.shared.infrastructure.persistence.outbox.OutboxRecordEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Outbox del contexto Identidad, en su propio esquema. */
@Entity(name = "IdentityOutboxRecord")
@Table(schema = "identity", name = "outbox")
public class IdentityOutboxRecord extends OutboxRecordEntity {
}
