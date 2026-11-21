package com.redsaludandina.compartido.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/** Reloj y generador de ids deterministas para las pruebas de aplicación. */
public final class Fijos {

    public static final Instant AHORA = Instant.parse("2026-11-20T15:00:00Z");
    public static final Clock RELOJ = Clock.fixed(AHORA, ZoneOffset.UTC);

    private Fijos() {
    }

    /** UUID 00000000-0000-0000-0000-000000000001, ...0002, ... */
    public static Supplier<UUID> idsSecuenciales() {
        AtomicLong siguiente = new AtomicLong();
        return () -> new UUID(0, siguiente.incrementAndGet());
    }
}
