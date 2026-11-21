package com.redsaludandina.insights.infrastructure.persistence;

import com.redsaludandina.shared.infrastructure.persistence.inbox.InboxRecordEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Inbox de las proyecciones de analítica, en el esquema {@code insights}. */
@Entity(name = "InsightsInboxRecord")
@Table(schema = "insights", name = "inbox")
public class InsightsInboxRecord extends InboxRecordEntity {
}
