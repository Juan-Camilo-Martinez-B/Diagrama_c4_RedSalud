package com.redsaludandina.interconsulta.aplicacion.puerto.entrada;

import com.redsaludandina.compartido.dominio.IdClinico;

/** Cierra una interconsulta atendida y publica InterconsultaCerrada. */
public interface CerrarInterconsulta {

    void cerrar(IdClinico interconsultaId);
}
