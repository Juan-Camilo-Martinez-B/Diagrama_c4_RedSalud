package com.redsaludandina.legado.dominio.modelo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class HistorialClinicoTest {

    private static final Instant OBTENIDO = Instant.parse("2026-11-20T02:00:00Z");

    @Test
    void laCopiaValeHastaQueVenceSuTtl() {
        MarcaFrescura frescura = MarcaFrescura.conTtl(OBTENIDO, Duration.ofHours(24), OrigenHistorial.READ_MODEL);
        HistorialClinico historial = new HistorialClinico("pac-1", List.of(), List.of(), frescura);

        assertThat(historial.caducaEn()).isEqualTo(OBTENIDO.plus(Duration.ofHours(24)));
        assertThat(historial.estaVigente(OBTENIDO.plus(Duration.ofHours(23)))).isTrue();
        assertThat(historial.estaVigente(OBTENIDO.plus(Duration.ofHours(24)))).isFalse();
    }

    @Test
    void elTtlDebeSerPositivo() {
        assertThatThrownBy(() -> MarcaFrescura.conTtl(OBTENIDO, Duration.ZERO, OrigenHistorial.LEGADO_DIRECTO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void muestraEpisodiosYDocumentosDelMasRecienteAlMasAntiguo() {
        HistorialClinico historial = new HistorialClinico("pac-1",
                List.of(new EpisodioHistorico("E-1", LocalDate.of(2024, 3, 1), "Control"),
                        new EpisodioHistorico("E-2", LocalDate.of(2025, 7, 9), "Urgencia")),
                List.of(new DocumentoDisponible("D-1", "Laboratorio", LocalDate.of(2023, 1, 5)),
                        new DocumentoDisponible("D-2", "Radiografía", LocalDate.of(2025, 7, 9))),
                MarcaFrescura.conTtl(OBTENIDO, Duration.ofHours(1), OrigenHistorial.READ_MODEL));

        assertThat(historial.episodios()).extracting(EpisodioHistorico::referencia).containsExactly("E-2", "E-1");
        assertThat(historial.documentos()).extracting(DocumentoDisponible::referencia).containsExactly("D-2", "D-1");
    }
}
