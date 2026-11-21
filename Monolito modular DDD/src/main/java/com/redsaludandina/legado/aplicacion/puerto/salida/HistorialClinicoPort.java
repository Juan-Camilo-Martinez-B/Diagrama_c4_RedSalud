package com.redsaludandina.legado.aplicacion.puerto.salida;

import com.redsaludandina.legado.dominio.modelo.HistorialClinico;
import java.util.Optional;

/**
 * El historial en el lenguaje del dominio. Solo lo implementa la capa anticorrupción
 * (AdaptadorHistorial): primero el read model (p95 ≤ 2 s) y, si falta, la BD de solo lectura del
 * legado (≤ 6 s). Vacío si el legado no tiene historial para el paciente.
 */
public interface HistorialClinicoPort {

    Optional<HistorialClinico> obtener(String pacienteId);
}
