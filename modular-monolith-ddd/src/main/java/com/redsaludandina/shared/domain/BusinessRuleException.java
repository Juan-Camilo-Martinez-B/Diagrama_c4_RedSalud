package com.redsaludandina.shared.domain;

/**
 * Una regla de negocio impidió la operación. El {@link #code()} es estable para que los adaptadores
 * lo traduzcan (por ejemplo, la sincronización offline lo devuelve como «rechazado»).
 */
public class BusinessRuleException extends RuntimeException {

    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = Require.text(code, "code");
    }

    public String code() {
        return code;
    }
}
