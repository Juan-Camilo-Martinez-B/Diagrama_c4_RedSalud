package com.redsaludandina.analitica.dominio.modelo;

import static com.redsaludandina.compartido.dominio.IdsDePrueba.id;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.redsaludandina.compartido.evento.CupoReservado;
import com.redsaludandina.compartido.evento.InterconsultaCerrada;
import com.redsaludandina.compartido.evento.InterconsultaSolicitada;
import com.redsaludandina.compartido.evento.PacienteAtendido;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class IndicadorUsoTest {

    private static final Instant T = Instant.parse("2026-11-10T08:00:00Z");

    private static CupoReservado reserva(String centro, Duration anticipacion) {
        return new CupoReservado(id(1), id(2), "pac-1", centro, "esp-1", T.plus(anticipacion), T);
    }

    @Test
    void calculaEsperaPromedioTeleconsultasYTasaDeCierre() {
        IndicadorUso indicador = IndicadorUso.iniciar("centro-a", YearMonth.of(2026, 11));

        indicador.proyectar(reserva("centro-a", Duration.ofHours(2)));
        indicador.proyectar(reserva("centro-a", Duration.ofHours(4)));
        indicador.proyectar(new PacienteAtendido(id(3), "pac-1", id(1), "centro-a", T));
        indicador.proyectar(new InterconsultaSolicitada(id(4), "centro-a", "Cardiología", T));
        indicador.proyectar(new InterconsultaSolicitada(id(5), "centro-a", "Cardiología", T));
        indicador.proyectar(new InterconsultaCerrada(id(4), "centro-a", T));

        assertThat(indicador.tiempoEsperaPromedio()).contains(Duration.ofHours(3));
        assertThat(indicador.teleconsultas()).isEqualTo(1);
        assertThat(indicador.tasaCierre()).contains(0.5);
    }

    @Test
    void sinDatosNoInventaPromediosNiTasas() {
        IndicadorUso indicador = IndicadorUso.iniciar("centro-a", YearMonth.of(2026, 11));

        assertThat(indicador.tiempoEsperaPromedio()).isEmpty();
        assertThat(indicador.tasaCierre()).isEmpty();
    }

    @Test
    void rechazaEventosDeOtroCentro() {
        IndicadorUso indicador = IndicadorUso.iniciar("centro-a", YearMonth.of(2026, 11));

        assertThatThrownBy(() -> indicador.proyectar(reserva("centro-b", Duration.ofHours(1))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(indicador.reservas()).isZero();
    }
}
