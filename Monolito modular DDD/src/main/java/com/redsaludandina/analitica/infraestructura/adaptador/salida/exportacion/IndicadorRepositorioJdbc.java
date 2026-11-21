package com.redsaludandina.analitica.infraestructura.adaptador.salida.exportacion;

import com.redsaludandina.analitica.aplicacion.puerto.salida.IndicadorRepositorio;
import com.redsaludandina.analitica.dominio.modelo.IndicadorUso;
import java.time.Duration;
import java.time.YearMonth;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Indicadores en analitica.indicador, separados de los esquemas transaccionales. En AWS se
 * exportan a S3 para consultarlos con Athena (Caso §6).
 */
public final class IndicadorRepositorioJdbc implements IndicadorRepositorio {

    private final JdbcClient jdbc;

    public IndicadorRepositorioJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<IndicadorUso> buscar(String centroId, YearMonth periodo) {
        return jdbc.sql("SELECT teleconsultas, reservas, espera_acumulada_segundos, interconsultas_solicitadas,"
                        + " interconsultas_cerradas FROM analitica.indicador WHERE centro_id = :centro AND periodo = :periodo")
                .param("centro", centroId)
                .param("periodo", periodo.toString())
                .query((fila, n) -> IndicadorUso.reconstituir(centroId, periodo, fila.getLong("teleconsultas"),
                        fila.getLong("reservas"), Duration.ofSeconds(fila.getLong("espera_acumulada_segundos")),
                        fila.getLong("interconsultas_solicitadas"), fila.getLong("interconsultas_cerradas")))
                .optional();
    }

    @Override
    public void guardar(IndicadorUso indicador) {
        int actualizados = jdbc.sql("UPDATE analitica.indicador SET teleconsultas = :teleconsultas, reservas = :reservas,"
                        + " espera_acumulada_segundos = :espera, interconsultas_solicitadas = :solicitadas,"
                        + " interconsultas_cerradas = :cerradas WHERE centro_id = :centro AND periodo = :periodo")
                .paramSource(parametros(indicador))
                .update();
        if (actualizados == 0) {
            jdbc.sql("INSERT INTO analitica.indicador (centro_id, periodo, teleconsultas, reservas,"
                            + " espera_acumulada_segundos, interconsultas_solicitadas, interconsultas_cerradas)"
                            + " VALUES (:centro, :periodo, :teleconsultas, :reservas, :espera, :solicitadas, :cerradas)")
                    .paramSource(parametros(indicador))
                    .update();
        }
    }

    private static java.util.Map<String, Object> parametros(IndicadorUso indicador) {
        return java.util.Map.of(
                "centro", indicador.centroId(),
                "periodo", indicador.periodo().toString(),
                "teleconsultas", indicador.teleconsultas(),
                "reservas", indicador.reservas(),
                "espera", indicador.esperaAcumulada().toSeconds(),
                "solicitadas", indicador.interconsultasSolicitadas(),
                "cerradas", indicador.interconsultasCerradas());
    }
}
