package com.redsaludandina.shared.infrastructure.web;

/**
 * Encabezados HTTP propios del sistema. {@link #USER} identifica al usuario SOLO en el perfil local y
 * en las pruebas; con Cognito activo se ignora y manda el {@code sub} del JWT.
 */
public final class Headers {

    public static final String USER = "X-User-Id";
    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    /** Frescura visible de la copia de lectura del legado (Caso §3). */
    public static final String DATA_FRESHNESS = "X-Data-Freshness";

    private Headers() {
    }
}
