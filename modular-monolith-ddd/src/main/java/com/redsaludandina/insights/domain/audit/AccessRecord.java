package com.redsaludandina.insights.domain.audit;

import com.redsaludandina.shared.domain.Require;
import com.redsaludandina.shared.event.AccessDenied;
import com.redsaludandina.shared.event.ClinicalRead;
import com.redsaludandina.shared.event.ClinicalValueDiscarded;
import java.time.Instant;
import java.util.UUID;

/**
 * Registro inmutable de la evidencia de accesos (Caso §5, ADR-04). Su id es el id del evento que lo
 * originó: un evento entregado dos veces produce el MISMO registro, no dos.
 */
public record AccessRecord(UUID id, AccessType type, String userId, String resource, String reason,
                           Instant occurredAt, String detail) {

    public static final String SYNC_SYSTEM_USER = "system:sync";

    public AccessRecord {
        Require.notNull(id, "id");
        Require.notNull(type, "type");
        userId = Require.text(userId, "userId");
        resource = Require.text(resource, "resource");
        reason = Require.text(reason, "reason");
        Require.notNull(occurredAt, "occurredAt");
        detail = detail == null ? "" : detail;
    }

    public static AccessRecord of(UUID eventId, ClinicalRead read) {
        return new AccessRecord(eventId, AccessType.CLINICAL_READ, read.userId(), read.resource(), read.reason(),
                read.occurredAt(), "");
    }

    public static AccessRecord of(UUID eventId, ClinicalValueDiscarded discarded) {
        String detail = "field=" + discarded.field()
                + "; kept=\"" + discarded.keptValue() + "\" (" + discarded.keptStamp() + ")"
                + "; discarded=\"" + discarded.discardedValue() + "\" (" + discarded.discardedStamp() + ")";
        return new AccessRecord(eventId, AccessType.CLINICAL_VALUE_DISCARDED, SYNC_SYSTEM_USER,
                "episode/" + discarded.episodeId(), "Resolución last-writer-wins", discarded.occurredAt(), detail);
    }

    public static AccessRecord of(UUID eventId, AccessDenied denied) {
        return new AccessRecord(eventId, AccessType.ACCESS_DENIED, denied.userId(), denied.resource(), denied.reason(),
                denied.occurredAt(), "");
    }
}
