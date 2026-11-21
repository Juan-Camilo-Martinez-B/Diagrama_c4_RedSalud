package com.redsaludandina.prescription.application.command;

import com.redsaludandina.shared.application.cqrs.Command;
import com.redsaludandina.shared.domain.ClinicalId;

public record AddMedicationCommand(ClinicalId prescriptionId, String name, String dose, String instructions)
        implements Command {
}
