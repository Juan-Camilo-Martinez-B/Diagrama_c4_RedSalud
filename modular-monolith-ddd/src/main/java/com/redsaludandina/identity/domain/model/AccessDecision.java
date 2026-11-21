package com.redsaludandina.identity.domain.model;

import com.redsaludandina.shared.domain.Require;

/** Resultado de la política de acceso, siempre con su razón (para auditar la negación). */
public record AccessDecision(boolean allowed, String reason) {

    public AccessDecision {
        reason = Require.text(reason, "reason");
    }

    public static AccessDecision allow(String reason) {
        return new AccessDecision(true, reason);
    }

    public static AccessDecision deny(String reason) {
        return new AccessDecision(false, reason);
    }
}
