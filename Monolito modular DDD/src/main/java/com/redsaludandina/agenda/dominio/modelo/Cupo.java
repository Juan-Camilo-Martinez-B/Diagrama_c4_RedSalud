package com.redsaludandina.agenda.dominio.modelo;

import com.redsaludandina.compartido.dominio.AgregadoRaiz;
import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.compartido.dominio.ReglaDeNegocioException;
import com.redsaludandina.compartido.dominio.Requisitos;
import com.redsaludandina.compartido.evento.CupoReservado;
import java.time.Instant;
import java.util.Optional;

/**
 * Raíz del agregado Cupo (contexto Agenda). Garantiza la regla «sin doble reserva» con
 * consistencia fuerte (Caso §2): un cupo tiene como máximo una reserva. Los conflictos de cupos
 * no se resuelven en el borde: se rechazan y el centro reintenta contra el servidor (Caso §4).
 */
public final class Cupo extends AgregadoRaiz<IdClinico> {

    public static final String CUPO_YA_RESERVADO = "CUPO_YA_RESERVADO";
    public static final String FRANJA_YA_EMPEZO = "FRANJA_YA_EMPEZO";

    private final IdClinico id;
    private final String especialistaId;
    private final Franja franja;
    private Reserva reserva;

    private Cupo(IdClinico id, String especialistaId, Franja franja, Reserva reserva) {
        this.id = Requisitos.noNulo(id, "cupoId");
        this.especialistaId = Requisitos.texto(especialistaId, "especialistaId");
        this.franja = Requisitos.noNulo(franja, "franja");
        this.reserva = reserva;
    }

    /** Publica un cupo nuevo y disponible en la agenda de un especialista. */
    public static Cupo publicar(IdClinico id, String especialistaId, Franja franja) {
        return new Cupo(id, especialistaId, franja, null);
    }

    /** Reconstruye un cupo ya guardado; no emite eventos. */
    public static Cupo reconstituir(IdClinico id, String especialistaId, Franja franja, Reserva reserva) {
        return new Cupo(id, especialistaId, franja, reserva);
    }

    /**
     * Reserva el cupo para un paciente. Reenviar la MISMA reserva (mismo id generado en el cliente)
     * no tiene efecto, para que la sincronización sea idempotente; una reserva distinta se rechaza.
     */
    public void reservar(IdClinico reservaId, String pacienteId, String centroId, Instant ahora) {
        Requisitos.noNulo(reservaId, "reservaId");
        Requisitos.noNulo(ahora, "ahora");
        if (reserva != null) {
            if (reserva.id().equals(reservaId)) {
                return;
            }
            throw new ReglaDeNegocioException(CUPO_YA_RESERVADO,
                    "El cupo " + id + " ya está reservado; no se admite doble reserva");
        }
        if (franja.yaEmpezo(ahora)) {
            throw new ReglaDeNegocioException(FRANJA_YA_EMPEZO,
                    "El cupo " + id + " ya empezó y no se puede reservar");
        }
        reserva = new Reserva(reservaId, pacienteId, centroId, ahora);
        registrar(new CupoReservado(id, reservaId, reserva.pacienteId(), reserva.centroId(),
                especialistaId, franja.inicio(), ahora));
    }

    public EstadoCupo estado() {
        return reserva == null ? EstadoCupo.DISPONIBLE : EstadoCupo.RESERVADO;
    }

    @Override
    public IdClinico id() {
        return id;
    }

    public String especialistaId() {
        return especialistaId;
    }

    public Franja franja() {
        return franja;
    }

    public Optional<Reserva> reserva() {
        return Optional.ofNullable(reserva);
    }
}
