package com.redsaludandina.prescription.application.command;

import com.redsaludandina.shared.application.cqrs.Command;
import com.redsaludandina.shared.domain.ClinicalId;

/** El médico autenticado empieza una receta para un episodio. */
public record DraftPrescriptionCommand(ClinicalId prescriptionId, ClinicalId episodeId, String prescriberId)
        implements Command {
}
