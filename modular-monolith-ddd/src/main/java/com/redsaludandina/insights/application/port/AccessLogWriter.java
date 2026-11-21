package com.redsaludandina.insights.application.port;

import com.redsaludandina.insights.domain.audit.AccessRecord;

/** Evidencia append-only de accesos (puerto de salida): en AWS, S3 Object Lock en modo compliance. */
public interface AccessLogWriter {

    /** {@code false} si ya existía un registro con ese id (entrega repetida del mismo evento). */
    boolean appendIfAbsent(AccessRecord accessRecord);
}
