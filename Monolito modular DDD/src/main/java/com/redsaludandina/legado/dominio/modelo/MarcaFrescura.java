package com.redsaludandina.legado.dominio.modelo;

import com.redsaludandina.compartido.dominio.Requisitos;
import java.time.Duration;
import java.time.Instant;

/**
 * Cuándo se obtuvo el historial del legado y hasta cuándo vale la copia. Es la «fecha de
 * caducidad visible» de la vista de lectura, con TTL explícito (Caso §2 y §3).
 */
public record MarcaFrescura(Instant obtenidoEn, Instant expiraEn, OrigenHistorial origen) {

    public MarcaFrescura {
        Requisitos.noNulo(obtenidoEn, "obtenidoEn");
        Requisitos.noNulo(expiraEn, "expiraEn");
        Requisitos.noNulo(origen, "origen");
        if (!expiraEn.isAfter(obtenidoEn)) {
            throw new IllegalArgumentException("La copia del historial debe caducar después de obtenerse");
        }
    }

    public static MarcaFrescura conTtl(Instant obtenidoEn, Duration ttl, OrigenHistorial origen) {
        Requisitos.noNulo(ttl, "ttl");
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("El TTL del historial debe ser positivo");
        }
        return new MarcaFrescura(obtenidoEn, Requisitos.noNulo(obtenidoEn, "obtenidoEn").plus(ttl), origen);
    }

    public boolean estaVigente(Instant ahora) {
        return Requisitos.noNulo(ahora, "ahora").isBefore(expiraEn);
    }
}
