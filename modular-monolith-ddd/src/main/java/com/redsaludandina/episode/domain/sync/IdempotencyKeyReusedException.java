package com.redsaludandina.episode.domain.sync;

import com.redsaludandina.shared.domain.BusinessRuleException;

/** La misma Idempotency-Key llegó con OTRO lote: es un error del cliente (HTTP 422). */
public class IdempotencyKeyReusedException extends BusinessRuleException {

    public static final String CODE = "IDEMPOTENCY_KEY_REUSED";

    public IdempotencyKeyReusedException(String idempotencyKey) {
        super(CODE, "La Idempotency-Key " + idempotencyKey + " ya se usó con otro lote");
    }
}
