package com.redsaludandina.interconsulta.aplicacion.servicio;

import static com.redsaludandina.compartido.dominio.IdsDePrueba.id;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.redsaludandina.compartido.aplicacion.AlmacenEnMemoria;
import com.redsaludandina.compartido.aplicacion.Fijos;
import com.redsaludandina.compartido.aplicacion.UnidadDeTrabajoDirecta;
import com.redsaludandina.compartido.aplicacion.mensajeria.EventoEntrante;
import com.redsaludandina.compartido.aplicacion.mensajeria.EventoFueraDeOrdenException;
import com.redsaludandina.compartido.aplicacion.mensajeria.InboxEnMemoria;
import com.redsaludandina.compartido.aplicacion.mensajeria.OutboxEnMemoria;
import com.redsaludandina.compartido.dominio.EventoDominio;
import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.compartido.evento.CupoReservado;
import com.redsaludandina.compartido.evento.InterconsultaCerrada;
import com.redsaludandina.compartido.evento.PacienteAtendido;
import com.redsaludandina.interconsulta.aplicacion.puerto.entrada.SolicitarInterconsulta;
import com.redsaludandina.interconsulta.aplicacion.puerto.salida.InterconsultaRepositorio;
import com.redsaludandina.interconsulta.dominio.modelo.EstadoInterconsulta;
import com.redsaludandina.interconsulta.dominio.modelo.Interconsulta;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ServicioInterconsultaTest {

    private final AlmacenEnMemoria<IdClinico, Interconsulta> almacen = new AlmacenEnMemoria<>(Interconsulta::id);
    private final OutboxEnMemoria outbox = new OutboxEnMemoria();

    private final ServicioInterconsulta servicio = new ServicioInterconsulta(new InterconsultaRepositorio() {
        @Override
        public Optional<Interconsulta> buscar(IdClinico interconsultaId) {
            return almacen.buscar(interconsultaId);
        }

        @Override
        public void guardar(Interconsulta interconsulta) {
            almacen.guardar(interconsulta);
        }

        @Override
        public Optional<Interconsulta> buscarPorCupo(IdClinico cupoId) {
            return almacen.todos().stream().filter(i -> i.cupoId().filter(cupoId::equals).isPresent()).findFirst();
        }

        @Override
        public List<Interconsulta> solicitadasDelPaciente(String pacienteId) {
            return almacen.todos().stream()
                    .filter(i -> i.pacienteId().equals(pacienteId) && i.estado() == EstadoInterconsulta.SOLICITADA)
                    .sorted(Comparator.comparing(i -> i.alertas().get(0).creadaEn()))
                    .toList();
        }

        @Override
        public List<Interconsulta> conAlertasPendientes(String centroId) {
            return almacen.todos().stream()
                    .filter(i -> i.centroId().equals(centroId) && !i.alertasPendientes().isEmpty())
                    .toList();
        }
    }, new InboxEnMemoria(), new UnidadDeTrabajoDirecta(), outbox.registrador("interconsulta"), Fijos.RELOJ);

    private static EventoEntrante entrante(int n, EventoDominio evento) {
        return new EventoEntrante(new UUID(1, n), evento);
    }

    private static CupoReservado cupoReservado(String pacienteId, IdClinico cupoId) {
        return new CupoReservado(cupoId, id(50), pacienteId, "centro-a", "esp-1", Fijos.AHORA.plusSeconds(3600), Fijos.AHORA);
    }

    private static PacienteAtendido pacienteAtendido(String pacienteId, IdClinico cupoId) {
        return new PacienteAtendido(id(60), pacienteId, cupoId, "centro-a", Fijos.AHORA);
    }

    private void solicitar() {
        servicio.solicitar(new SolicitarInterconsulta.Comando(id(1), id(2), "pac-1", "centro-a", "Cardiología", "Soplo"));
    }

    @Test
    void avanzaConLosEventosDeAgendaYEpisodioAunqueLleguenRepetidos() {
        solicitar();

        servicio.procesar(entrante(1, cupoReservado("pac-1", id(10))));
        servicio.procesar(entrante(1, cupoReservado("pac-1", id(10))));
        servicio.procesar(entrante(2, pacienteAtendido("pac-1", id(10))));
        servicio.procesar(entrante(2, pacienteAtendido("pac-1", id(10))));
        servicio.cerrar(id(1));

        assertThat(almacen.buscar(id(1))).get().satisfies(i -> assertThat(i.estado()).isEqualTo(EstadoInterconsulta.CERRADA));
        assertThat(outbox.eventosDe(InterconsultaCerrada.class)).hasSize(1);
        assertThat(servicio.pendientes("centro-a")).isEmpty();
    }

    @Test
    void unPacienteAtendidoQueLlegaAntesQueSuCupoReservadoSePideReintentar() {
        solicitar();

        assertThatThrownBy(() -> servicio.procesar(entrante(2, pacienteAtendido("pac-1", id(10)))))
                .isInstanceOf(EventoFueraDeOrdenException.class);
        assertThat(almacen.buscar(id(1))).get().satisfies(i -> assertThat(i.estado()).isEqualTo(EstadoInterconsulta.SOLICITADA));
    }

    @Test
    void lasReservasYAtencionesSinInterconsultaSeIgnoran() {
        servicio.procesar(entrante(1, cupoReservado("pac-9", id(11))));
        servicio.procesar(entrante(2, pacienteAtendido("pac-9", id(11))));

        assertThat(almacen.todos()).isEmpty();
        assertThat(outbox.mensajes()).isEmpty();
    }

    @Test
    void laVistaDeAlertasMuestraLasPendientesDelCentro() {
        solicitar();

        assertThat(servicio.pendientes("centro-a")).extracting(Interconsulta::id).containsExactly(id(1));
        assertThat(servicio.pendientes("centro-b")).isEmpty();
    }
}
