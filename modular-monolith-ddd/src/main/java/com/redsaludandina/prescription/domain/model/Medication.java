package com.redsaludandina.prescription.domain.model;

import com.redsaludandina.shared.domain.Require;

/** Línea de la receta: medicamento, dosis e indicación. */
public record Medication(String name, String dose, String instructions) {

    public Medication {
        name = Require.text(name, "nombre del medicamento");
        dose = Require.text(dose, "dosis");
        instructions = Require.text(instructions, "indicación");
    }
}
