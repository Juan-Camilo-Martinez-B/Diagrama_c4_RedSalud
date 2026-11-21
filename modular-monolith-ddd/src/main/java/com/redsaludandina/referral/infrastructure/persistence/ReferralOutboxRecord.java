package com.redsaludandina.referral.infrastructure.persistence;

import com.redsaludandina.shared.infrastructure.persistence.outbox.OutboxRecordEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Outbox del contexto Interconsulta, en su propio esquema. */
@Entity(name = "ReferralOutboxRecord")
@Table(schema = "referral", name = "outbox")
public class ReferralOutboxRecord extends OutboxRecordEntity {
}
