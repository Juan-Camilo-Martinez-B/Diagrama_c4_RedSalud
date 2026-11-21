package com.redsaludandina.prescription.domain.model;

import com.redsaludandina.shared.domain.Require;
import java.time.Instant;

/** Firma digital de la receta: quién firmó, cuándo y el valor de la firma. */
public record DigitalSignature(String signerId, Instant signedAt, String value) {

    public DigitalSignature {
        signerId = Require.text(signerId, "signerId");
        Require.notNull(signedAt, "signedAt");
        value = Require.text(value, "valor de la firma");
    }

    /** Dos firmas son la misma si las hizo el mismo firmante con el mismo valor (reintento idempotente). */
    public boolean sameAs(DigitalSignature other) {
        return signerId.equals(other.signerId) && value.equals(other.value);
    }
}
