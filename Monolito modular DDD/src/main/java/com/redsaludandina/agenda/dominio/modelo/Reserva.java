package com.redsaludandina.agenda.dominio.modelo;

import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.compartido.dominio.Requisitos;
import java.time.Instant;
import java.util.Objects;

/** Ocupación de un cupo por un paciente. Solo existe dentro de su {@link Cupo}. */
public final class Reserva {

    private final IdClinico id;
    private final String pacienteId;
    private final String centroId;
    private final Instant reservadaEn;

    public Reserva(IdClinico id, String pacienteId, String centroId, Instant reservadaEn) {
        this.id = Requisitos.noNulo(id, "reservaId");
        this.pacienteId = Requisitos.texto(pacienteId, "pacienteId");
        this.centroId = Requisitos.texto(centroId, "centroId");
        this.reservadaEn = Requisitos.noNulo(reservadaEn, "reservadaEn");
    }

    public IdClinico id() {
        return id;
    }

    public String pacienteId() {
        return pacienteId;
    }

    public String centroId() {
        return centroId;
    }

    public Instant reservadaEn() {
        return reservadaEn;
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof Reserva reserva && id.equals(reserva.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
