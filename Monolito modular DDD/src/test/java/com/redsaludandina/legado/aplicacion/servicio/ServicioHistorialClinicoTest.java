package com.redsaludandina.legado.aplicacion.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.redsaludandina.compartido.aplicacion.AccesoNoAutorizadoException;
import com.redsaludandina.compartido.aplicacion.Fijos;
import com.redsaludandina.compartido.aplicacion.RecursoNoEncontradoException;
import com.redsaludandina.compartido.aplicacion.UnidadDeTrabajoDirecta;
import com.redsaludandina.compartido.aplicacion.mensajeria.OutboxEnMemoria;
import com.redsaludandina.compartido.evento.LecturaClinica;
import com.redsaludandina.identidad.aplicacion.puerto.entrada.AutorizarAcceso;
import com.redsaludandina.identidad.aplicacion.puerto.entrada.AutorizarAcceso.ResultadoAutorizacion;
import com.redsaludandina.legado.aplicacion.puerto.entrada.ConsultarHistorialClinico.Consulta;
import com.redsaludandina.legado.aplicacion.puerto.entrada.ConsultarHistorialClinico.VistaHistorial;
import com.redsaludandina.legado.dominio.modelo.EpisodioHistorico;
import com.redsaludandina.legado.dominio.modelo.HistorialClinico;
import com.redsaludandina.legado.dominio.modelo.MarcaFrescura;
import com.redsaludandina.legado.dominio.modelo.OrigenHistorial;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ServicioHistorialClinicoTest {

    private final OutboxEnMemoria outbox = new OutboxEnMemoria();
    private final HistorialClinico historial = new HistorialClinico("pac-1",
            List.of(new EpisodioHistorico("E-1", LocalDate.of(2025, 7, 9), "Urgencia")), List.of(),
            MarcaFrescura.conTtl(Fijos.AHORA.minus(Duration.ofHours(1)), Duration.ofHours(24), OrigenHistorial.READ_MODEL));

    private ServicioHistorialClinico servicio(boolean permitido) {
        AutorizarAcceso autorizacion = solicitud -> new ResultadoAutorizacion(permitido, permitido ? "ok" : "sin relación");
        return new ServicioHistorialClinico(autorizacion,
                pacienteId -> pacienteId.equals("pac-1") ? Optional.of(historial) : Optional.empty(),
                new UnidadDeTrabajoDirecta(), outbox.registrador("legado"), Fijos.RELOJ);
    }

    @Test
    void devuelveLaVistaConSuFrescuraYDejaLaLecturaParaAuditoria() {
        VistaHistorial vista = servicio(true).consultar(new Consulta("esp-1", "pac-1", "centro-a", "Interconsulta"));

        assertThat(vista.episodios()).extracting(e -> e.referencia()).containsExactly("E-1");
        assertThat(vista.vigente()).isTrue();
        assertThat(vista.origen()).isEqualTo("READ_MODEL");
        assertThat(vista.caducaEn()).isEqualTo(historial.caducaEn());
        assertThat(outbox.eventosDe(LecturaClinica.class)).singleElement().satisfies(lectura -> {
            assertThat(lectura.recurso()).isEqualTo("historial/pac-1");
            assertThat(lectura.motivo()).isEqualTo("Interconsulta");
        });
    }

    @Test
    void siNoEstaAutorizadoNoLeeNiRegistraLectura() {
        assertThatThrownBy(() -> servicio(false).consultar(new Consulta("esp-1", "pac-1", "centro-a", "Curiosidad")))
                .isInstanceOf(AccesoNoAutorizadoException.class);
        assertThat(outbox.mensajes()).isEmpty();
    }

    @Test
    void sinMotivoNoHayLectura() {
        assertThatThrownBy(() -> servicio(true).consultar(new Consulta("esp-1", "pac-1", "centro-a", " ")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(outbox.mensajes()).isEmpty();
    }

    @Test
    void unPacienteSinHistorialEnElLegadoNoDejaLectura() {
        assertThatThrownBy(() -> servicio(true).consultar(new Consulta("esp-1", "pac-9", "centro-a", "Control")))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThat(outbox.mensajes()).isEmpty();
    }
}
