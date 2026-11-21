package com.redsaludandina;

import com.redsaludandina.shared.domain.ClinicalId;

/** ULID válidos y deterministas para las pruebas. */
public final class TestIds {

    private TestIds() {
    }

    public static ClinicalId id(int n) {
        return ClinicalId.of(String.format("01ARZ3NDEKTSV4RRFFQ69G%04d", n));
    }

    public static String ulid(int n) {
        return id(n).value();
    }
}
