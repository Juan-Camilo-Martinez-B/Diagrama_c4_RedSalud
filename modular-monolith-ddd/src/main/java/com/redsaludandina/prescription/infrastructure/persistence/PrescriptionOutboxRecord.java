package com.redsaludandina.prescription.infrastructure.persistence;

import com.redsaludandina.shared.infrastructure.persistence.outbox.OutboxRecordEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Outbox del contexto Receta, en su propio esquema. */
@Entity(name = "PrescriptionOutboxRecord")
@Table(schema = "prescription", name = "outbox")
public class PrescriptionOutboxRecord extends OutboxRecordEntity {
}
