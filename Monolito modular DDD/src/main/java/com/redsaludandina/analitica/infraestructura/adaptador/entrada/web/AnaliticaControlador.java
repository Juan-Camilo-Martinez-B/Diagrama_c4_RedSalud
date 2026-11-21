package com.redsaludandina.analitica.infraestructura.adaptador.entrada.web;

import com.redsaludandina.analitica.aplicacion.puerto.entrada.ConsultarIndicador;
import com.redsaludandina.compartido.aplicacion.RecursoNoEncontradoException;
import java.time.Duration;
import java.time.YearMonth;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Tablero del gestor: indicadores de un centro en un mes (yyyy-MM). */
@RestController
public class AnaliticaControlador {

    private final ConsultarIndicador consultarIndicador;

    public AnaliticaControlador(ConsultarIndicador consultarIndicador) {
        this.consultarIndicador = consultarIndicador;
    }

    /** {@code esperaPromedioMinutos} y {@code tasaCierre} son nulos si todavía no hay datos. */
    record VistaIndicador(String centroId, String periodo, long teleconsultas, long reservas, Long esperaPromedioMinutos,
                          long interconsultasSolicitadas, long interconsultasCerradas, Double tasaCierre) {
    }

    @GetMapping("/analitica/indicadores/{centroId}/{periodo}")
    VistaIndicador consultar(@PathVariable String centroId, @PathVariable String periodo) {
        return consultarIndicador.consultar(centroId, YearMonth.parse(periodo))
                .map(indicador -> new VistaIndicador(indicador.centroId(), indicador.periodo().toString(),
                        indicador.teleconsultas(), indicador.reservas(),
                        indicador.tiempoEsperaPromedio().map(Duration::toMinutes).orElse(null),
                        indicador.interconsultasSolicitadas(), indicador.interconsultasCerradas(),
                        indicador.tasaCierre().orElse(null)))
                .orElseThrow(() -> new RecursoNoEncontradoException("indicador de " + centroId + " en " + periodo));
    }
}
