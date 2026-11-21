package com.redsaludandina.agenda.dominio.modelo;

import static com.redsaludandina.compartido.dominio.IdsDePrueba.id;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.redsaludandina.compartido.dominio.ReglaDeNegocioException;
import com.redsaludandina.compartido.evento.CupoReservado;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CupoTest {

    private static final Instant INICIO = Instant.parse("2026-12-01T14:00:00Z");
    private static final Instant ANTES = INICIO.minusSeconds(86_400);

    private Cupo cupoDisponible() {
        return Cupo.publicar(id(1), "esp-cardio-1", new Franja(INICIO, INICIO.plusSeconds(1_800)));
    }

    @Test
    void reservarUnCupoDisponibleLoOcupaYPublicaCupoReservado() {
        Cupo cupo = cupoDisponible();

        cupo.reservar(id(10), "pac-1", "centro-a", ANTES);

        assertThat(cupo.estado()).isEqualTo(EstadoCupo.RESERVADO);
        assertThat(cupo.extraerEventos()).singleElement()
                .isInstanceOfSatisfying(CupoReservado.class, evento -> {
                    assertThat(evento.cupoId()).isEqualTo(id(1));
                    assertThat(evento.reservaId()).isEqualTo(id(10));
                    assertThat(evento.inicioFranja()).isEqualTo(INICIO);
                    assertThat(evento.ocurridoEn()).isEqualTo(ANTES);
                });
    }

    @Test
    void noAdmiteDobleReserva() {
        Cupo cupo = cupoDisponible();
        cupo.reservar(id(10), "pac-1", "centro-a", ANTES);

        assertThatThrownBy(() -> cupo.reservar(id(11), "pac-2", "centro-b", ANTES))
                .isInstanceOfSatisfying(ReglaDeNegocioException.class,
                        error -> assertThat(error.codigo()).isEqualTo(Cupo.CUPO_YA_RESERVADO));
        assertThat(cupo.reserva()).get().extracting(Reserva::pacienteId).isEqualTo("pac-1");
    }

    @Test
    void reenviarLaMismaReservaEsIdempotente() {
        Cupo cupo = cupoDisponible();
        cupo.reservar(id(10), "pac-1", "centro-a", ANTES);
        cupo.extraerEventos();

        cupo.reservar(id(10), "pac-1", "centro-a", ANTES.plusSeconds(60));

        assertThat(cupo.extraerEventos()).isEmpty();
        assertThat(cupo.reserva()).get().extracting(Reserva::reservadaEn).isEqualTo(ANTES);
    }

    @Test
    void noSeReservaUnCupoQueYaEmpezo() {
        Cupo cupo = cupoDisponible();

        assertThatThrownBy(() -> cupo.reservar(id(10), "pac-1", "centro-a", INICIO))
                .isInstanceOfSatisfying(ReglaDeNegocioException.class,
                        error -> assertThat(error.codigo()).isEqualTo(Cupo.FRANJA_YA_EMPEZO));
        assertThat(cupo.estado()).isEqualTo(EstadoCupo.DISPONIBLE);
    }

    @Test
    void unaFranjaDebeTerminarDespuesDeEmpezar() {
        assertThatThrownBy(() -> new Franja(INICIO, INICIO)).isInstanceOf(IllegalArgumentException.class);
    }
}
