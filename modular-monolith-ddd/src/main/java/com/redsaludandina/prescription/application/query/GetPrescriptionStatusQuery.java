package com.redsaludandina.prescription.application.query;

import com.redsaludandina.shared.application.cqrs.Query;

/** ¿En qué estado está la receta y ya llegó a la farmacia? */
public record GetPrescriptionStatusQuery(String prescriptionId) implements Query<PrescriptionView> {
}
