package com.redsaludandina.prescription.application.query;

import com.redsaludandina.prescription.application.port.PrescriptionReadModel;
import com.redsaludandina.shared.application.ResourceNotFoundException;
import com.redsaludandina.shared.application.cqrs.QueryHandler;
import com.redsaludandina.shared.domain.Require;

public final class GetPrescriptionStatusHandler implements QueryHandler<GetPrescriptionStatusQuery, PrescriptionView> {

    private final PrescriptionReadModel readModel;

    public GetPrescriptionStatusHandler(PrescriptionReadModel readModel) {
        this.readModel = Require.notNull(readModel, "readModel");
    }

    @Override
    public PrescriptionView handle(GetPrescriptionStatusQuery query) {
        String prescriptionId = Require.text(query.prescriptionId(), "prescriptionId");
        return readModel.findById(prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("la receta " + prescriptionId));
    }
}
