package com.redsaludandina.referral.infrastructure.persistence;

import com.redsaludandina.shared.infrastructure.persistence.inbox.InboxRecordEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Inbox de los consumidores de Interconsulta, en su propio esquema. */
@Entity(name = "ReferralInboxRecord")
@Table(schema = "referral", name = "inbox")
public class ReferralInboxRecord extends InboxRecordEntity {
}
