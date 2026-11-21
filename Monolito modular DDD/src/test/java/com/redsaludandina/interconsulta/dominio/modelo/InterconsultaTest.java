package com.redsaludandina.interconsulta.dominio.modelo;

import static com.redsaludandina.compartido.dominio.IdsDePrueba.id;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.redsaludandina.compartido.dominio.ReglaDeNegocioException;
import com.redsaludandina.compartido.evento.InterconsultaCerrada;
import com.redsaludandina.compartido.evento.InterconsultaSolicitada;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class InterconsultaTest {

    private static final Instant T = Instant.parse("2026-11-20T09:00:00Z");

    private Interconsulta solicitada() {
        return Interconsulta.solicitar(id(1), id(2), "pac-1", "centro-a", "Cardiología", "Soplo a estudio", T);
    }

    @Test
    void solicitarLaDejaPendienteDeAgendarYLoPublica() {
        Interconsulta interconsulta = solicitada();

        assertThat(interconsulta.estado()).isEqualTo(EstadoInterconsulta.SOLICITADA);
        assertThat(interconsulta.alertasPendientes()).extracting(Alerta::tipo)
                .containsExactly(TipoAlerta.PENDIENTE_AGENDAR);
        assertThat(interconsulta.extraerEventos()).singleElement().isInstanceOf(InterconsultaSolicitada.class);
    }

    @Test
    void recorreSolicitadaAgendadaAtendidaCerradaConUnaAlertaPorPaso() {
        Interconsulta interconsulta = solicitada();
        interconsulta.extraerEventos();

        interconsulta.agendar(id(10), T.plusSeconds(60));
        assertThat(interconsulta.alertasPendientes()).extracting(Alerta::tipo)
                .containsExactly(TipoAlerta.PENDIENTE_ATENCION);

        interconsulta.registrarAtencion(id(20), T.plusSeconds(120));
        assertThat(interconsulta.alertasPendientes()).extracting(Alerta::tipo)
                .containsExactly(TipoAlerta.PENDIENTE_CIERRE);

        interconsulta.cerrar(T.plusSeconds(180));

        assertThat(interconsulta.estado()).isEqualTo(EstadoInterconsulta.CERRADA);
        assertThat(interconsulta.alertasPendientes()).isEmpty();
        assertThat(interconsulta.alertas()).hasSize(3).allSatisfy(alerta -> assertThat(alerta.resueltaEn()).isPresent());
        assertThat(interconsulta.extraerEventos()).singleElement().isInstanceOf(InterconsultaCerrada.class);
    }

    @Test
    void noSeSaltaPasos() {
        Interconsulta interconsulta = solicitada();

        assertThatThrownBy(() -> interconsulta.registrarAtencion(id(20), T))
                .isInstanceOfSatisfying(ReglaDeNegocioException.class,
                        error -> assertThat(error.codigo()).isEqualTo(Interconsulta.TRANSICION_INVALIDA));
        assertThatThrownBy(() -> interconsulta.cerrar(T)).isInstanceOf(ReglaDeNegocioException.class);
        assertThat(interconsulta.estado()).isEqualTo(EstadoInterconsulta.SOLICITADA);
    }

    @Test
    void unEventoRepetidoNoLaHaceAvanzarDosVeces() {
        Interconsulta interconsulta = solicitada();
        interconsulta.agendar(id(10), T);

        interconsulta.agendar(id(10), T.plusSeconds(30));

        assertThat(interconsulta.estado()).isEqualTo(EstadoInterconsulta.AGENDADA);
        assertThat(interconsulta.alertas()).hasSize(2);
    }

    @Test
    void otroCupoParaUnaInterconsultaYaAgendadaSeRechaza() {
        Interconsulta interconsulta = solicitada();
        interconsulta.agendar(id(10), T);

        assertThatThrownBy(() -> interconsulta.agendar(id(11), T)).isInstanceOf(ReglaDeNegocioException.class);
        assertThat(interconsulta.cupoId()).contains(id(10));
    }
}
