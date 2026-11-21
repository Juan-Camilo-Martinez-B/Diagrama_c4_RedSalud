package com.redsaludandina.insights.application.event;

import com.redsaludandina.insights.application.port.AccessLogWriter;
import com.redsaludandina.insights.domain.audit.AccessRecord;
import com.redsaludandina.shared.application.messaging.EventConsumer;
import com.redsaludandina.shared.domain.DomainEvent;
import com.redsaludandina.shared.domain.Require;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * Consumidor de auditoría: convierte un evento en un registro inmutable de la evidencia. Es
 * idempotente por sí mismo (el id del registro es el id del evento), así que no necesita inbox.
 */
public final class AuditConsumer<E extends DomainEvent> implements EventConsumer<E> {

    private final String name;
    private final Class<E> eventType;
    private final BiFunction<UUID, E, AccessRecord> toRecord;
    private final AccessLogWriter accessLog;

    public AuditConsumer(String name, Class<E> eventType, BiFunction<UUID, E, AccessRecord> toRecord,
                         AccessLogWriter accessLog) {
        this.name = Require.text(name, "name");
        this.eventType = Require.notNull(eventType, "eventType");
        this.toRecord = Require.notNull(toRecord, "toRecord");
        this.accessLog = Require.notNull(accessLog, "accessLog");
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Class<E> eventType() {
        return eventType;
    }

    @Override
    public void consume(UUID eventId, E event) {
        accessLog.appendIfAbsent(toRecord.apply(eventId, event));
    }
}
