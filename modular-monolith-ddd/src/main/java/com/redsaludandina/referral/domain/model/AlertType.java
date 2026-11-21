package com.redsaludandina.referral.domain.model;

/** Alertas de una interconsulta que espera el siguiente paso (Caso §2: estados y alertas). */
public enum AlertType {
    PENDING_SCHEDULING,
    PENDING_ATTENTION,
    PENDING_CLOSURE
}
