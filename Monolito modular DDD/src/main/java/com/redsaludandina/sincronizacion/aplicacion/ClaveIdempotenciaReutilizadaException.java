package com.redsaludandina.sincronizacion.aplicacion;

/**
 * Llegó una Idempotency-Key ya usada con un lote DISTINTO. No se aplica nada ni se devuelve la
 * respuesta guardada: sería responder a otra pregunta.
 */
public class ClaveIdempotenciaReutilizadaException extends RuntimeException {

    public static final String CODIGO = "IDEMPOTENCY_KEY_REUTILIZADA";

    public ClaveIdempotenciaReutilizadaException(String clave) {
        super("La Idempotency-Key " + clave + " ya se usó con otro lote");
    }
}
