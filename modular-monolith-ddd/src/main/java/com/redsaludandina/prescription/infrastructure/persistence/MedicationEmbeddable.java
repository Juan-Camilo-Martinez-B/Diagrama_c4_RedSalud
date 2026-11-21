package com.redsaludandina.prescription.infrastructure.persistence;

import com.redsaludandina.prescription.domain.model.Medication;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Fila {@code prescription.medication}. */
@Embeddable
public class MedicationEmbeddable {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String dose;

    @Column(nullable = false)
    private String instructions;

    protected MedicationEmbeddable() {
    }

    MedicationEmbeddable(Medication medication) {
        this.name = medication.name();
        this.dose = medication.dose();
        this.instructions = medication.instructions();
    }

    Medication toDomain() {
        return new Medication(name, dose, instructions);
    }
}
