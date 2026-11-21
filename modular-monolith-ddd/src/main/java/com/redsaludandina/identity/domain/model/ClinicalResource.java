package com.redsaludandina.identity.domain.model;

import com.redsaludandina.shared.domain.Require;
import java.util.Optional;

/** Recurso clínico al que se pide acceso, con los atributos que la política necesita. */
public record ClinicalResource(String reference, String patientId, String centerId, String specialty) {

    public ClinicalResource {
        reference = Require.text(reference, "reference");
        patientId = Require.text(patientId, "patientId");
        centerId = Require.text(centerId, "centerId");
        specialty = Require.optionalText(specialty);
    }

    public Optional<String> requiredSpecialty() {
        return Optional.ofNullable(specialty);
    }
}
