package com.redsaludandina.prescription.application.port;

import com.redsaludandina.prescription.domain.model.Prescription;
import com.redsaludandina.shared.domain.ClinicalId;
import java.util.Optional;

/** Lado de escritura: carga y guarda el agregado {@link Prescription} (puerto de salida). */
public interface PrescriptionRepository {

    boolean exists(ClinicalId prescriptionId);

    Optional<Prescription> findById(ClinicalId prescriptionId);

    void save(Prescription prescription);
}
