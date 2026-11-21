package com.redsaludandina.interconsulta.aplicacion.puerto.entrada;

import com.redsaludandina.compartido.aplicacion.mensajeria.EventoEntrante;

/**
 * Política «Avanzar estado»: consume CupoReservado (→ agendada) y PacienteAtendido (→ atendida).
 * Puede lanzar EventoFueraDeOrdenException para que el consumidor reintente más tarde.
 */
public interface AvanzarInterconsulta {

    void procesar(EventoEntrante entrante);
}
