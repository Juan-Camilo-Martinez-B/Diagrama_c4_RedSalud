package com.redsaludandina.episodio.aplicacion.puerto.entrada;

import com.redsaludandina.compartido.dominio.IdClinico;

/** Cierra la teleconsulta y publica PacienteAtendido. */
public interface CerrarTeleconsulta {

    void cerrar(IdClinico episodioId);
}
