package com.redsaludandina.legacy.application.port;

import com.redsaludandina.legacy.domain.model.ClinicalHistory;
import com.redsaludandina.legacy.domain.model.FreshnessMark;
import java.util.Optional;

/**
 * El legado clínico visto desde el núcleo (puerto de salida). Solo la capa anticorrupción lo
 * implementa: consulta el legado y TRADUCE su modelo al canónico. Vacío si el paciente no existe allí;
 * {@link LegacyUnavailableException} si el legado no responde a tiempo.
 */
public interface LegacyHistorySource {

    Optional<ClinicalHistory> fetch(String patientId, FreshnessMark freshness);
}
