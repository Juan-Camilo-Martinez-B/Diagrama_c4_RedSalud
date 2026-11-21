package com.redsaludandina.prescription.application.command;

import com.redsaludandina.prescription.domain.model.Medication;
import com.redsaludandina.shared.application.cqrs.CommandHandler;
import com.redsaludandina.shared.domain.Require;

public final class AddMedicationHandler implements CommandHandler<AddMedicationCommand> {

    private final PrescriptionUpdater updater;

    public AddMedicationHandler(PrescriptionUpdater updater) {
        this.updater = Require.notNull(updater, "updater");
    }

    @Override
    public void handle(AddMedicationCommand command) {
        Medication medication = new Medication(command.name(), command.dose(), command.instructions());
        updater.update(command.prescriptionId(), prescription -> prescription.addMedication(medication));
    }
}
