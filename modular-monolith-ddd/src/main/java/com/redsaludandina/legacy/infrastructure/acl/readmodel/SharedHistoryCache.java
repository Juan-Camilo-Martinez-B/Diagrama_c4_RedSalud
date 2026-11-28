package com.redsaludandina.legacy.infrastructure.acl.readmodel;

import com.redsaludandina.legacy.application.query.ClinicalHistoryView;
import java.util.Optional;

/**
 * Caché compartida del historial (nivel 2). En un despliegue AWS este puerto lo cumple ElastiCache.
 * En local y en pruebas lo cumple una caché en el proceso, con el mismo contrato.
 */
public interface SharedHistoryCache {

    Optional<ClinicalHistoryView> find(String patientId);

    void put(String patientId, ClinicalHistoryView view);

    void invalidate(String patientId);
}
