package com.redsaludandina.prescription.application.query;

import java.time.Instant;

/** Modelo de lectura del estado de una receta (sin el detalle clínico de los medicamentos). */
public record PrescriptionView(String prescriptionId, String episodeId, String status, Integer medicationCount,
                               Instant signedAt, Instant sentToPharmacyAt) {
}
