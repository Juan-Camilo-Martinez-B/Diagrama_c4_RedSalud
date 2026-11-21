package com.redsaludandina.prescription.infrastructure.persistence;

import com.redsaludandina.shared.infrastructure.persistence.inbox.InboxRecordEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Inbox de los consumidores de Receta, en su propio esquema. */
@Entity(name = "PrescriptionInboxRecord")
@Table(schema = "prescription", name = "inbox")
public class PrescriptionInboxRecord extends InboxRecordEntity {
}
