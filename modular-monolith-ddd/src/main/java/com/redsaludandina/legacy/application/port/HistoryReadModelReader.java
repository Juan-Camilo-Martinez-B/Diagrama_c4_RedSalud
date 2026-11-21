package com.redsaludandina.legacy.application.port;

import com.redsaludandina.legacy.application.query.ClinicalHistoryView;
import java.util.Optional;

/** Lado de lectura del modelo de lectura del historial: DTO de lectura (puerto de salida). */
public interface HistoryReadModelReader {

    Optional<ClinicalHistoryView> find(String patientId);
}
