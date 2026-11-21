package com.redsaludandina.episodio.dominio.modelo;

import static com.redsaludandina.compartido.dominio.IdsDePrueba.id;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.redsaludandina.compartido.dominio.ReglaDeNegocioException;
import com.redsaludandina.compartido.dominio.SelloCentro;
import com.redsaludandina.compartido.evento.LecturaClinica;
import com.redsaludandina.compartido.evento.PacienteAtendido;
import com.redsaludandina.compartido.evento.ValorClinicoDescartado;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EpisodioTest {

    private static final Instant T = Instant.parse("2026-11-20T15:00:00Z");
    private static final String SHA = "a".repeat(64);

    private Episodio episodio() {
        return Episodio.abrir(id(1), "pac-1", id(2), "centro-a");
    }

    private static SelloCentro sello(String centro, long segundos) {
        return new SelloCentro(centro, T.plusSeconds(segundos));
    }

    @Test
    void elValorMasRecienteGanaYElAnteriorQuedaParaAuditoria() {
        Episodio episodio = episodio();
        episodio.registrarAsistencia(CampoAsistencia.HALLAZGOS, "Tos seca", sello("centro-a", 0), T);

        episodio.registrarAsistencia(CampoAsistencia.HALLAZGOS, "Tos productiva", sello("centro-b", 10), T);

        assertThat(episodio.asistencia().valor(CampoAsistencia.HALLAZGOS)).get()
                .extracting(ValorClinico::texto).isEqualTo("Tos productiva");
        assertThat(episodio.extraerEventos()).singleElement()
                .isInstanceOfSatisfying(ValorClinicoDescartado.class, evento -> {
                    assertThat(evento.valorConservado()).isEqualTo("Tos productiva");
                    assertThat(evento.valorDescartado()).isEqualTo("Tos seca");
                });
    }

    @Test
    void unValorQueLlegaTardeConSelloAntiguoNoPisaAlVigentePeroTampocoSePierde() {
        Episodio episodio = episodio();
        episodio.registrarAsistencia(CampoAsistencia.CONDUCTA, "Control en 7 días", sello("centro-a", 60), T);

        episodio.registrarAsistencia(CampoAsistencia.CONDUCTA, "Control en 30 días", sello("centro-b", 5), T);

        assertThat(episodio.asistencia().valor(CampoAsistencia.CONDUCTA)).get()
                .extracting(ValorClinico::texto).isEqualTo("Control en 7 días");
        assertThat(episodio.extraerEventos()).singleElement()
                .isInstanceOfSatisfying(ValorClinicoDescartado.class,
                        evento -> assertThat(evento.valorDescartado()).isEqualTo("Control en 30 días"));
    }

    @Test
    void cadaCampoSeResuelvePorSeparado() {
        Episodio episodio = episodio();
        episodio.registrarAsistencia(CampoAsistencia.MOTIVO, "Dolor torácico", sello("centro-a", 0), T);
        episodio.registrarAsistencia(CampoAsistencia.HALLAZGOS, "Sin soplos", sello("centro-b", 30), T);

        assertThat(episodio.asistencia().valores()).hasSize(2);
        assertThat(episodio.extraerEventos()).isEmpty();
    }

    @Test
    void reenviarElMismoValorNoProduceNada() {
        Episodio episodio = episodio();
        episodio.registrarAsistencia(CampoAsistencia.MOTIVO, "Fiebre", sello("centro-a", 0), T);

        episodio.registrarAsistencia(CampoAsistencia.MOTIVO, "Fiebre", sello("centro-a", 0), T);

        assertThat(episodio.extraerEventos()).isEmpty();
    }

    @Test
    void noSeCierraLaTeleconsultaSinMotivo() {
        Episodio episodio = episodio();

        assertThatThrownBy(() -> episodio.marcarAtendido(T))
                .isInstanceOfSatisfying(ReglaDeNegocioException.class,
                        error -> assertThat(error.codigo()).isEqualTo(Episodio.ASISTENCIA_SIN_MOTIVO));
        assertThat(episodio.estado()).isEqualTo(EstadoEpisodio.ABIERTO);
    }

    @Test
    void cerrarLaTeleconsultaPublicaPacienteAtendidoUnaSolaVez() {
        Episodio episodio = episodio();
        episodio.registrarAsistencia(CampoAsistencia.MOTIVO, "Fiebre", sello("centro-a", 0), T);

        episodio.marcarAtendido(T);
        episodio.marcarAtendido(T.plusSeconds(60));

        assertThat(episodio.estado()).isEqualTo(EstadoEpisodio.ATENDIDO);
        assertThat(episodio.extraerEventos()).singleElement()
                .isInstanceOfSatisfying(PacienteAtendido.class, evento -> {
                    assertThat(evento.cupo()).contains(id(2));
                    assertThat(evento.centroId()).isEqualTo("centro-a");
                });
    }

    @Test
    void unAdjuntoReenviadoNoSeDuplicaPeroOtroContenidoConElMismoIdSeRechaza() {
        Episodio episodio = episodio();
        episodio.adjuntar(new Adjunto(id(5), TipoAdjunto.EXAMEN, "adjuntos/5", SHA));

        episodio.adjuntar(new Adjunto(id(5), TipoAdjunto.EXAMEN, "adjuntos/5", SHA));

        assertThat(episodio.adjuntos()).hasSize(1);
        assertThatThrownBy(() -> episodio.adjuntar(new Adjunto(id(5), TipoAdjunto.IMAGEN, "adjuntos/5", "b".repeat(64))))
                .isInstanceOfSatisfying(ReglaDeNegocioException.class,
                        error -> assertThat(error.codigo()).isEqualTo(Episodio.ADJUNTO_EN_CONFLICTO));
    }

    @Test
    void cadaLecturaDejaRastroConMotivo() {
        Episodio episodio = episodio();

        episodio.registrarLectura("esp-1", "Interconsulta de cardiología", T);

        assertThat(episodio.extraerEventos()).singleElement()
                .isInstanceOfSatisfying(LecturaClinica.class, evento -> {
                    assertThat(evento.recurso()).isEqualTo("episodio/" + id(1));
                    assertThat(evento.motivo()).isEqualTo("Interconsulta de cardiología");
                });
        assertThatThrownBy(() -> episodio.registrarLectura("esp-1", " ", T))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
