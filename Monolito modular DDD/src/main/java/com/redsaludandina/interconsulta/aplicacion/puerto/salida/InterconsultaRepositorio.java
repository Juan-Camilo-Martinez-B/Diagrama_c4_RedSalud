package com.redsaludandina.interconsulta.aplicacion.puerto.salida;

import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.interconsulta.dominio.modelo.Interconsulta;
import java.util.List;
import java.util.Optional;

/** Interconsultas en Aurora PostgreSQL (esquema interconsulta). */
public interface InterconsultaRepositorio {

    Optional<Interconsulta> buscar(IdClinico interconsultaId);

    void guardar(Interconsulta interconsulta);

    /** La interconsulta que ya quedó agendada con ese cupo, si existe. */
    Optional<Interconsulta> buscarPorCupo(IdClinico cupoId);

    /** Interconsultas del paciente todavía en SOLICITADA, de la más antigua a la más reciente. */
    List<Interconsulta> solicitadasDelPaciente(String pacienteId);

    List<Interconsulta> conAlertasPendientes(String centroId);
}
