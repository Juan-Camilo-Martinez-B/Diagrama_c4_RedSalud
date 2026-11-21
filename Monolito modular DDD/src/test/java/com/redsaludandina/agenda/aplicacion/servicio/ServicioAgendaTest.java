package com.redsaludandina.agenda.aplicacion.servicio;

import static com.redsaludandina.compartido.dominio.IdsDePrueba.id;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.redsaludandina.agenda.aplicacion.puerto.entrada.PublicarCupo;
import com.redsaludandina.agenda.aplicacion.puerto.entrada.ReservarCupo;
import com.redsaludandina.agenda.aplicacion.puerto.salida.CupoRepositorio;
import com.redsaludandina.agenda.dominio.modelo.Cupo;
import com.redsaludandina.compartido.aplicacion.AlmacenEnMemoria;
import com.redsaludandina.compartido.aplicacion.Fijos;
import com.redsaludandina.compartido.aplicacion.RecursoNoEncontradoException;
import com.redsaludandina.compartido.aplicacion.UnidadDeTrabajoDirecta;
import com.redsaludandina.compartido.aplicacion.mensajeria.OutboxEnMemoria;
import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.compartido.dominio.ReglaDeNegocioException;
import com.redsaludandina.compartido.evento.CupoReservado;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ServicioAgendaTest {

    private final AlmacenEnMemoria<IdClinico, Cupo> almacen = new AlmacenEnMemoria<>(Cupo::id);
    private final OutboxEnMemoria outbox = new OutboxEnMemoria();
    private final ServicioAgenda servicio = new ServicioAgenda(new CupoRepositorio() {
        @Override
        public Optional<Cupo> buscar(IdClinico cupoId) {
            return almacen.buscar(cupoId);
        }

        @Override
        public void guardar(Cupo cupo) {
            almacen.guardar(cupo);
        }
    }, new UnidadDeTrabajoDirecta(), outbox.registrador("agenda"), Fijos.RELOJ);

    private void publicarCupoDeManana() {
        servicio.publicar(new PublicarCupo.Comando(id(1), "esp-1", Fijos.AHORA.plus(Duration.ofDays(1)),
                Fijos.AHORA.plus(Duration.ofDays(1)).plus(Duration.ofMinutes(30))));
    }

    @Test
    void reservarGuardaElCupoYDejaCupoReservadoEnElOutbox() {
        publicarCupoDeManana();

        servicio.reservar(new ReservarCupo.Comando(id(1), id(10), "pac-1", "centro-a"));

        assertThat(almacen.buscar(id(1))).get().satisfies(cupo -> assertThat(cupo.reserva()).isPresent());
        assertThat(outbox.mensajes()).singleElement().satisfies(mensaje -> {
            assertThat(mensaje.contexto()).isEqualTo("agenda");
            assertThat(mensaje.agregadoId()).isEqualTo(id(1).valor());
            assertThat(mensaje.evento()).isInstanceOf(CupoReservado.class);
        });
    }

    @Test
    void publicarDosVecesElMismoCupoNoLoDuplicaNiLoLibera() {
        publicarCupoDeManana();
        servicio.reservar(new ReservarCupo.Comando(id(1), id(10), "pac-1", "centro-a"));

        publicarCupoDeManana();

        assertThat(almacen.todos()).hasSize(1);
        assertThat(almacen.buscar(id(1))).get().satisfies(cupo -> assertThat(cupo.reserva()).isPresent());
    }

    @Test
    void unaDobleReservaSeRechazaSinDejarEventos() {
        publicarCupoDeManana();
        servicio.reservar(new ReservarCupo.Comando(id(1), id(10), "pac-1", "centro-a"));

        assertThatThrownBy(() -> servicio.reservar(new ReservarCupo.Comando(id(1), id(11), "pac-2", "centro-b")))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThat(outbox.mensajes()).hasSize(1);
    }

    @Test
    void reservarUnCupoInexistenteFalla() {
        assertThatThrownBy(() -> servicio.reservar(new ReservarCupo.Comando(id(9), id(10), "pac-1", "centro-a")))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
