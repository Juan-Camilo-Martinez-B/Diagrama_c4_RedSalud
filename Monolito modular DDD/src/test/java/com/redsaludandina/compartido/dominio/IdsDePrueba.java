package com.redsaludandina.compartido.dominio;

/** ULIDs válidos y legibles para las pruebas: {@code id(1)}, {@code id(2)}... */
public final class IdsDePrueba {

    private IdsDePrueba() {
    }

    public static IdClinico id(int n) {
        return IdClinico.de(String.format("01ARZ3NDEKTSV4RRFFQ69G%04d", n));
    }
}
