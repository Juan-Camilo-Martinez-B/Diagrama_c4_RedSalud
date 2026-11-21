package com.redsaludandina.prescription.infrastructure.persistence;

import com.redsaludandina.prescription.application.port.PrescriptionReadModel;
import com.redsaludandina.prescription.application.query.PrescriptionView;
import java.util.Optional;

/** Adaptador del lado de lectura de Receta. */
public final class JpaPrescriptionReadModel implements PrescriptionReadModel {

    private final PrescriptionQueryRepository queries;

    public JpaPrescriptionReadModel(PrescriptionQueryRepository queries) {
        this.queries = queries;
    }

    @Override
    public Optional<PrescriptionView> findById(String prescriptionId) {
        return queries.findView(prescriptionId);
    }
}
