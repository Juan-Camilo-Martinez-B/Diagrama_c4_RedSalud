package com.redsaludandina.prescription.application.command;

import com.redsaludandina.shared.application.cqrs.Command;
import com.redsaludandina.shared.domain.ClinicalId;

/** Firma la receta en el servidor (nunca offline); a partir de aquí es inmutable. */
public record SignPrescriptionCommand(ClinicalId prescriptionId, String signerId, String signatureValue)
        implements Command {
}
