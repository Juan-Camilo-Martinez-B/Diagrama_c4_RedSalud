package com.redsaludandina.episode.domain.sync;

import com.redsaludandina.shared.domain.Require;

/** Resultado de un cambio del lote, con el código de la regla que lo rechazó o lo dejó pendiente. */
public record ItemResult(String changeId, ItemStatus status, String code, String message) {

    public static final String INVALID_DATA = "INVALID_DATA";
    public static final String UNAVAILABLE = "UNAVAILABLE";
    public static final String ORDER_PENDING = "ORDER_PENDING";
    public static final String REQUIRES_SERVER = "REQUIRES_SERVER";

    public ItemResult {
        changeId = Require.text(changeId, "changeId");
        Require.notNull(status, "status");
    }

    public static ItemResult applied(String changeId) {
        return new ItemResult(changeId, ItemStatus.APPLIED, null, null);
    }

    public static ItemResult alreadyApplied(String changeId) {
        return new ItemResult(changeId, ItemStatus.ALREADY_APPLIED, null, "Otro intento ya lo había aplicado");
    }

    public static ItemResult rejected(String changeId, String code, String message) {
        return new ItemResult(changeId, ItemStatus.REJECTED, code, message);
    }

    public static ItemResult pending(String changeId, String code, String message) {
        return new ItemResult(changeId, ItemStatus.PENDING, code, message);
    }

    public boolean isPending() {
        return status == ItemStatus.PENDING;
    }
}
