package com.redsaludandina.prescription.application.command;

import com.redsaludandina.prescription.application.port.PrescriptionRepository;
import com.redsaludandina.prescription.domain.model.Prescription;
import com.redsaludandina.shared.application.UnitOfWork;
import com.redsaludandina.shared.application.cqrs.CommandHandler;
import com.redsaludandina.shared.domain.Require;

/** Crear dos veces la misma receta (mismo ULID) no la duplica. */
public final class DraftPrescriptionHandler implements CommandHandler<DraftPrescriptionCommand> {

    private final PrescriptionRepository prescriptions;
    private final UnitOfWork unitOfWork;

    public DraftPrescriptionHandler(PrescriptionRepository prescriptions, UnitOfWork unitOfWork) {
        this.prescriptions = Require.notNull(prescriptions, "prescriptions");
        this.unitOfWork = Require.notNull(unitOfWork, "unitOfWork");
    }

    @Override
    public void handle(DraftPrescriptionCommand command) {
        Prescription prescription = Prescription.draft(command.prescriptionId(), command.episodeId(),
                command.prescriberId());
        unitOfWork.run(() -> {
            if (!prescriptions.exists(prescription.id())) {
                prescriptions.save(prescription);
            }
        });
    }
}
