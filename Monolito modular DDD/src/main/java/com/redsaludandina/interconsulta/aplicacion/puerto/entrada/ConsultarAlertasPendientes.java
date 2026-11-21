package com.redsaludandina.interconsulta.aplicacion.puerto.entrada;

import com.redsaludandina.interconsulta.dominio.modelo.Interconsulta;
import java.util.List;

/** Vista «Alertas pendientes»: interconsultas de un centro que esperan un paso. */
public interface ConsultarAlertasPendientes {

    List<Interconsulta> pendientes(String centroId);
}
