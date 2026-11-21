package com.redsaludandina.insights.domain.audit;

/** Qué quedó registrado en la evidencia de accesos. */
public enum AccessType {
    /** Alguien leyó historia clínica (usuario, fecha, recurso y motivo). */
    CLINICAL_READ,
    /** Last-writer-wins descartó un valor clínico; se guardan ambos valores. */
    CLINICAL_VALUE_DISCARDED,
    /** La política de acceso negó un intento. */
    ACCESS_DENIED
}
