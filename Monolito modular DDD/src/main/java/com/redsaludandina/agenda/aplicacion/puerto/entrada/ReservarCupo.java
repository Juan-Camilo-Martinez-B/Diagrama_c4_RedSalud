package com.redsaludandina.agenda.aplicacion.puerto.entrada;

import com.redsaludandina.compartido.dominio.IdClinico;

/**
 * Reserva un cupo. {@code reservaId} lo genera el cliente: reenviar la misma reserva no la
 * duplica; otra reserva sobre un cupo ocupado se rechaza (CUPO_YA_RESERVADO).
 */
public interface ReservarCupo {

    void reservar(Comando comando);

    record Comando(IdClinico cupoId, IdClinico reservaId, String pacienteId, String centroId) {
    }
}
