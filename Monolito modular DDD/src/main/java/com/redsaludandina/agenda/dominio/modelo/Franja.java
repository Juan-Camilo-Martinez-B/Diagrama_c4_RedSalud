package com.redsaludandina.agenda.dominio.modelo;

import com.redsaludandina.compartido.dominio.Requisitos;
import java.time.Instant;

/** Intervalo de atención de un especialista. */
public record Franja(Instant inicio, Instant fin) {

    public Franja {
        Requisitos.noNulo(inicio, "inicio");
        Requisitos.noNulo(fin, "fin");
        if (!fin.isAfter(inicio)) {
            throw new IllegalArgumentException("La franja debe terminar después de empezar");
        }
    }

    public boolean yaEmpezo(Instant ahora) {
        return !ahora.isBefore(inicio);
    }
}
