package com.redsaludandina.prescription.application.port;

import com.redsaludandina.prescription.application.query.PrescriptionView;
import java.util.Optional;

/** Lado de lectura de Receta: DTO de lectura (puerto de salida). */
public interface PrescriptionReadModel {

    Optional<PrescriptionView> findById(String prescriptionId);
}
