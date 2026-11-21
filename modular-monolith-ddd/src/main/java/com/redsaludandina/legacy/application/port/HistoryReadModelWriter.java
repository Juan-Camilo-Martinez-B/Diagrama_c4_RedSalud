package com.redsaludandina.legacy.application.port;

import com.redsaludandina.legacy.domain.model.ClinicalHistory;
import com.redsaludandina.legacy.domain.model.FreshnessMark;
import java.util.Optional;

/** Lado de escritura del modelo de lectura del historial (puerto de salida). */
public interface HistoryReadModelWriter {

    Optional<FreshnessMark> freshnessOf(String patientId);

    void save(ClinicalHistory history);
}
