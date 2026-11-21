package com.redsaludandina.agenda.aplicacion.puerto.salida;

import com.redsaludandina.agenda.dominio.modelo.Cupo;
import com.redsaludandina.compartido.dominio.IdClinico;
import java.util.Optional;

/**
 * Cupos en Aurora PostgreSQL (esquema agenda). El adaptador aplica bloqueo optimista para que dos
 * reservas simultáneas no ganen las dos (consistencia fuerte, sin doble reserva).
 */
public interface CupoRepositorio {

    Optional<Cupo> buscar(IdClinico cupoId);

    void guardar(Cupo cupo);
}
