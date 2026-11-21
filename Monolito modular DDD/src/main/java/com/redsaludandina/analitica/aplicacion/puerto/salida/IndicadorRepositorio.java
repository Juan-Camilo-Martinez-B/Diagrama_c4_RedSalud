package com.redsaludandina.analitica.aplicacion.puerto.salida;

import com.redsaludandina.analitica.dominio.modelo.IndicadorUso;
import java.time.YearMonth;
import java.util.Optional;

/** Indicadores de uso, fuera de la base transaccional (export a S3 + Athena). */
public interface IndicadorRepositorio {

    Optional<IndicadorUso> buscar(String centroId, YearMonth periodo);

    void guardar(IndicadorUso indicador);
}
