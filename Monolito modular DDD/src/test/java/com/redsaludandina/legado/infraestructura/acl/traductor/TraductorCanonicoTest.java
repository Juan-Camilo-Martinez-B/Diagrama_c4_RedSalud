package com.redsaludandina.legado.infraestructura.acl.traductor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.redsaludandina.legado.dominio.modelo.DocumentoDisponible;
import com.redsaludandina.legado.dominio.modelo.EpisodioHistorico;
import com.redsaludandina.legado.dominio.modelo.HistorialClinico;
import com.redsaludandina.legado.dominio.modelo.MarcaFrescura;
import com.redsaludandina.legado.dominio.modelo.OrigenHistorial;
import com.redsaludandina.legado.infraestructura.acl.modelo.HistorialLegado;
import com.redsaludandina.legado.infraestructura.acl.modelo.HistorialLegado.AtencionLegado;
import com.redsaludandina.legado.infraestructura.acl.modelo.HistorialLegado.DocumentoLegado;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class TraductorCanonicoTest {

    private final TraductorCanonico traductor = new TraductorCanonico();
    private final MarcaFrescura frescura = MarcaFrescura.conTtl(Instant.parse("2026-11-20T02:00:00Z"),
            Duration.ofHours(24), OrigenHistorial.LEGADO_DIRECTO);

    @Test
    void traduceFechasCodigosYGlosasDelLegadoAlModeloCanonico() {
        HistorialLegado legado = new HistorialLegado(" pac-1 ",
                List.of(new AtencionLegado("AT-1", "20250709", "  "), new AtencionLegado("AT-2", "20240301", "Control ")),
                List.of(new DocumentoLegado("D-1", "lab", "20250709"), new DocumentoLegado("D-2", "TAC", "20240301")));

        HistorialClinico historial = traductor.traducir(legado, frescura);

        assertThat(historial.pacienteId()).isEqualTo("pac-1");
        assertThat(historial.episodios()).containsExactly(
                new EpisodioHistorico("AT-1", LocalDate.of(2025, 7, 9), "(sin glosa)"),
                new EpisodioHistorico("AT-2", LocalDate.of(2024, 3, 1), "Control"));
        assertThat(historial.documentos()).extracting(DocumentoDisponible::tipo).containsExactly("Laboratorio", "Otro (TAC)");
        assertThat(historial.frescura()).isEqualTo(frescura);
    }

    @Test
    void unaFechaMalFormadaEnElLegadoNoSeDejaPasar() {
        HistorialLegado legado = new HistorialLegado("pac-1", List.of(new AtencionLegado("AT-1", "2025-07-09", "x")), List.of());

        assertThatThrownBy(() -> traductor.traducir(legado, frescura)).isInstanceOf(IllegalStateException.class);
    }
}
