package com.redsaludandina.interconsulta.dominio.modelo;

import com.redsaludandina.compartido.dominio.Requisitos;
import java.time.Instant;
import java.util.Optional;

/** Pendiente de una interconsulta que alimenta la vista «Alertas pendientes». */
public final class Alerta {

    private final TipoAlerta tipo;
    private final Instant creadaEn;
    private Instant resueltaEn;

    public Alerta(TipoAlerta tipo, Instant creadaEn, Instant resueltaEn) {
        this.tipo = Requisitos.noNulo(tipo, "tipo");
        this.creadaEn = Requisitos.noNulo(creadaEn, "creadaEn");
        this.resueltaEn = resueltaEn;
    }

    static Alerta abrir(TipoAlerta tipo, Instant ahora) {
        return new Alerta(tipo, ahora, null);
    }

    void resolver(Instant ahora) {
        if (resueltaEn == null) {
            resueltaEn = ahora;
        }
    }

    public boolean estaPendiente() {
        return resueltaEn == null;
    }

    public TipoAlerta tipo() {
        return tipo;
    }

    public Instant creadaEn() {
        return creadaEn;
    }

    public Optional<Instant> resueltaEn() {
        return Optional.ofNullable(resueltaEn);
    }
}
