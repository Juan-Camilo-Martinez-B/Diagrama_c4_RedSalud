package com.redsaludandina.analitica.dominio.modelo;

import com.redsaludandina.compartido.dominio.EventoDominio;
import com.redsaludandina.compartido.dominio.Requisitos;
import com.redsaludandina.compartido.evento.CupoReservado;
import com.redsaludandina.compartido.evento.InterconsultaCerrada;
import com.redsaludandina.compartido.evento.InterconsultaSolicitada;
import com.redsaludandina.compartido.evento.PacienteAtendido;
import java.time.Duration;
import java.time.YearMonth;
import java.util.Optional;

/**
 * Raíz del agregado de Analítica de uso: indicadores de un centro en un mes (teleconsultas, tiempo
 * de espera, tasa de cierre), proyectados desde los eventos y nunca desde la base transaccional
 * (Caso §6).
 *
 * <p>Seudonimizado por diseño: solo guarda conteos y duraciones por centro; ningún identificador
 * de paciente entra en el indicador.
 */
public final class IndicadorUso {

    private final String centroId;
    private final YearMonth periodo;
    private long teleconsultas;
    private long reservas;
    private Duration esperaAcumulada;
    private long interconsultasSolicitadas;
    private long interconsultasCerradas;

    private IndicadorUso(String centroId, YearMonth periodo, long teleconsultas, long reservas,
                         Duration esperaAcumulada, long interconsultasSolicitadas, long interconsultasCerradas) {
        this.centroId = Requisitos.texto(centroId, "centroId");
        this.periodo = Requisitos.noNulo(periodo, "periodo");
        this.teleconsultas = teleconsultas;
        this.reservas = reservas;
        this.esperaAcumulada = Requisitos.noNulo(esperaAcumulada, "esperaAcumulada");
        this.interconsultasSolicitadas = interconsultasSolicitadas;
        this.interconsultasCerradas = interconsultasCerradas;
    }

    public static IndicadorUso iniciar(String centroId, YearMonth periodo) {
        return new IndicadorUso(centroId, periodo, 0, 0, Duration.ZERO, 0, 0);
    }

    public static IndicadorUso reconstituir(String centroId, YearMonth periodo, long teleconsultas, long reservas,
                                            Duration esperaAcumulada, long interconsultasSolicitadas,
                                            long interconsultasCerradas) {
        return new IndicadorUso(centroId, periodo, teleconsultas, reservas, esperaAcumulada,
                interconsultasSolicitadas, interconsultasCerradas);
    }

    /**
     * Acumula un evento de dominio del centro. Un evento de otro centro es un error de
     * enrutamiento y se rechaza; los eventos que no miden nada se ignoran.
     */
    public void proyectar(EventoDominio evento) {
        Requisitos.noNulo(evento, "evento");
        if (evento instanceof CupoReservado reservado) {
            exigirCentro(reservado.centroId());
            reservas++;
            Duration espera = Duration.between(reservado.ocurridoEn(), reservado.inicioFranja());
            esperaAcumulada = esperaAcumulada.plus(espera.isNegative() ? Duration.ZERO : espera);
        } else if (evento instanceof PacienteAtendido atendido) {
            exigirCentro(atendido.centroId());
            teleconsultas++;
        } else if (evento instanceof InterconsultaSolicitada solicitada) {
            exigirCentro(solicitada.centroId());
            interconsultasSolicitadas++;
        } else if (evento instanceof InterconsultaCerrada cerrada) {
            exigirCentro(cerrada.centroId());
            interconsultasCerradas++;
        }
    }

    private void exigirCentro(String centroDelEvento) {
        if (!centroId.equals(centroDelEvento)) {
            throw new IllegalArgumentException(
                    "El evento es del centro " + centroDelEvento + " y el indicador del centro " + centroId);
        }
    }

    /** Espera media entre reservar y la hora del cupo; vacío si aún no hay reservas. */
    public Optional<Duration> tiempoEsperaPromedio() {
        return reservas == 0 ? Optional.empty() : Optional.of(esperaAcumulada.dividedBy(reservas));
    }

    /** Proporción de interconsultas cerradas sobre las solicitadas; vacío si no hay solicitudes. */
    public Optional<Double> tasaCierre() {
        return interconsultasSolicitadas == 0
                ? Optional.empty()
                : Optional.of((double) interconsultasCerradas / interconsultasSolicitadas);
    }

    public String centroId() {
        return centroId;
    }

    public YearMonth periodo() {
        return periodo;
    }

    public long teleconsultas() {
        return teleconsultas;
    }

    public long reservas() {
        return reservas;
    }

    public Duration esperaAcumulada() {
        return esperaAcumulada;
    }

    public long interconsultasSolicitadas() {
        return interconsultasSolicitadas;
    }

    public long interconsultasCerradas() {
        return interconsultasCerradas;
    }
}
