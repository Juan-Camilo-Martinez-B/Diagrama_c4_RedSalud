package com.redsaludandina.episodio.aplicacion.servicio;

import static com.redsaludandina.compartido.dominio.IdsDePrueba.id;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.redsaludandina.compartido.aplicacion.AccesoNoAutorizadoException;
import com.redsaludandina.compartido.aplicacion.AlmacenEnMemoria;
import com.redsaludandina.compartido.aplicacion.Fijos;
import com.redsaludandina.compartido.aplicacion.UnidadDeTrabajoDirecta;
import com.redsaludandina.compartido.aplicacion.mensajeria.OutboxEnMemoria;
import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.compartido.evento.LecturaClinica;
import com.redsaludandina.compartido.evento.PacienteAtendido;
import com.redsaludandina.compartido.evento.ValorClinicoDescartado;
import com.redsaludandina.episodio.aplicacion.AlmacenAdjuntosDePrueba;
import com.redsaludandina.episodio.aplicacion.puerto.entrada.AbrirEpisodio;
import com.redsaludandina.episodio.aplicacion.puerto.entrada.ConsultarEpisodio;
import com.redsaludandina.episodio.aplicacion.puerto.entrada.RegistrarAsistencia;
import com.redsaludandina.episodio.aplicacion.puerto.salida.EpisodioRepositorio;
import com.redsaludandina.episodio.dominio.modelo.Episodio;
import com.redsaludandina.identidad.aplicacion.puerto.entrada.AutorizarAcceso;
import com.redsaludandina.identidad.aplicacion.puerto.entrada.AutorizarAcceso.ResultadoAutorizacion;
import com.redsaludandina.identidad.aplicacion.puerto.entrada.AutorizarAcceso.SolicitudAcceso;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ServicioEpisodioTest {

    private final AlmacenEnMemoria<IdClinico, Episodio> almacen = new AlmacenEnMemoria<>(Episodio::id);
    private final OutboxEnMemoria outbox = new OutboxEnMemoria();
    private final List<SolicitudAcceso> solicitudes = new ArrayList<>();
    private boolean permitir = true;

    private final AutorizarAcceso autorizacion = solicitud -> {
        solicitudes.add(solicitud);
        return new ResultadoAutorizacion(permitir, permitir ? "ok" : "El recurso es de otro centro");
    };

    private final ServicioEpisodio servicio = new ServicioEpisodio(new EpisodioRepositorio() {
        @Override
        public Optional<Episodio> buscar(IdClinico episodioId) {
            return almacen.buscar(episodioId);
        }

        @Override
        public void guardar(Episodio episodio) {
            almacen.guardar(episodio);
        }
    }, new AlmacenAdjuntosDePrueba(), autorizacion, new UnidadDeTrabajoDirecta(), outbox.registrador("episodio"), Fijos.RELOJ);

    private void abrir() {
        servicio.abrir(new AbrirEpisodio.Comando(id(1), "pac-1", id(2), "centro-a"));
    }

    private void registrar(String campo, String texto, String centro, long segundos) {
        servicio.registrar(new RegistrarAsistencia.Comando(id(1), campo, texto, centro,
                Fijos.AHORA.plus(Duration.ofSeconds(segundos))));
    }

    @Test
    void unConflictoOfflineDejaElValorDescartadoEnElOutbox() {
        abrir();
        registrar("hallazgos", "Tos seca", "centro-a", 0);

        registrar("HALLAZGOS", "Tos productiva", "centro-b", 30);

        assertThat(outbox.eventosDe(ValorClinicoDescartado.class)).singleElement()
                .satisfies(evento -> assertThat(evento.valorDescartado()).isEqualTo("Tos seca"));
    }

    @Test
    void cerrarDejaPacienteAtendidoEnElOutbox() {
        abrir();
        registrar("motivo", "Fiebre", "centro-a", 0);

        servicio.cerrar(id(1));

        assertThat(outbox.eventosDe(PacienteAtendido.class)).singleElement()
                .satisfies(evento -> assertThat(evento.cupo()).contains(id(2)));
    }

    @Test
    void consultarAutorizaConLosAtributosDelEpisodioYRegistraLaLectura() {
        abrir();

        servicio.consultar(new ConsultarEpisodio.Consulta(id(1), "med-1", "Control"));

        assertThat(solicitudes).singleElement().satisfies(solicitud -> {
            assertThat(solicitud.pacienteId()).isEqualTo("pac-1");
            assertThat(solicitud.centroId()).isEqualTo("centro-a");
            assertThat(solicitud.recurso()).isEqualTo("episodio/" + id(1));
        });
        assertThat(outbox.eventosDe(LecturaClinica.class)).singleElement()
                .satisfies(lectura -> assertThat(lectura.usuarioId()).isEqualTo("med-1"));
    }

    @Test
    void unaLecturaDenegadaNoDejaLecturaClinica() {
        abrir();
        permitir = false;

        assertThatThrownBy(() -> servicio.consultar(new ConsultarEpisodio.Consulta(id(1), "med-2", "Control")))
                .isInstanceOf(AccesoNoAutorizadoException.class);
        assertThat(outbox.eventosDe(LecturaClinica.class)).isEmpty();
    }

    @Test
    void unCampoDesconocidoEsUnDatoInvalido() {
        abrir();

        assertThatThrownBy(() -> registrar("diagnostico", "X", "centro-a", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static final String SHA = "a".repeat(64);

    @Test
    void unAdjuntoSoloSeRegistraConLaClaveQueAsignaElServidor() {
        abrir();

        assertThatThrownBy(() -> servicio.adjuntar(new com.redsaludandina.episodio.aplicacion.puerto.entrada.AdjuntarArchivo.Comando(
                id(1), id(5), "EXAMEN", "episodios/OTRO-PACIENTE/adjuntos/x", SHA)))
                .isInstanceOf(IllegalArgumentException.class);

        servicio.adjuntar(new com.redsaludandina.episodio.aplicacion.puerto.entrada.AdjuntarArchivo.Comando(
                id(1), id(5), "EXAMEN", "episodios/" + id(1) + "/adjuntos/" + id(5), SHA));
        assertThat(almacen.buscar(id(1))).get().satisfies(episodio -> assertThat(episodio.adjuntos()).hasSize(1));
    }

    @Test
    void descargarUnAdjuntoAutorizaYDejaLaLecturaDelAdjunto() {
        abrir();
        servicio.adjuntar(new com.redsaludandina.episodio.aplicacion.puerto.entrada.AdjuntarArchivo.Comando(
                id(1), id(5), "IMAGEN", "episodios/" + id(1) + "/adjuntos/" + id(5), SHA));

        var descarga = servicio.descargar(new com.redsaludandina.episodio.aplicacion.puerto.entrada.DescargarAdjunto.Consulta(
                id(1), id(5), "esp-1", "Interconsulta"));

        assertThat(descarga.url().toString()).endsWith("/adjuntos/" + id(5));
        assertThat(outbox.eventosDe(LecturaClinica.class)).singleElement()
                .satisfies(lectura -> assertThat(lectura.recurso()).isEqualTo("episodio/" + id(1) + "/adjunto/" + id(5)));
    }

    @Test
    void unaDescargaDenegadaNoFirmaNiAudita() {
        abrir();
        servicio.adjuntar(new com.redsaludandina.episodio.aplicacion.puerto.entrada.AdjuntarArchivo.Comando(
                id(1), id(5), "IMAGEN", "episodios/" + id(1) + "/adjuntos/" + id(5), SHA));
        permitir = false;

        assertThatThrownBy(() -> servicio.descargar(new com.redsaludandina.episodio.aplicacion.puerto.entrada.DescargarAdjunto.Consulta(
                id(1), id(5), "esp-9", "Curiosidad")))
                .isInstanceOf(AccesoNoAutorizadoException.class);
        assertThat(outbox.eventosDe(LecturaClinica.class)).isEmpty();
    }
}
