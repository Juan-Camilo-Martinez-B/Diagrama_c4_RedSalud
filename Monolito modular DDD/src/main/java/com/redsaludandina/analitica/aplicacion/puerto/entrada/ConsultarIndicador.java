package com.redsaludandina.analitica.aplicacion.puerto.entrada;

import com.redsaludandina.analitica.dominio.modelo.IndicadorUso;
import java.time.YearMonth;
import java.util.Optional;

/** Tablero del gestor: indicadores de un centro en un mes. */
public interface ConsultarIndicador {

    Optional<IndicadorUso> consultar(String centroId, YearMonth periodo);
}
