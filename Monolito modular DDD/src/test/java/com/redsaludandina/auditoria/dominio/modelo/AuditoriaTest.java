package com.redsaludandina.auditoria.dominio.modelo;

import static com.redsaludandina.compartido.dominio.IdsDePrueba.id;
import static org.assertj.core.api.Assertions.assertThat;

import com.redsaludandina.compartido.dominio.SelloCentro;
import com.redsaludandina.compartido.evento.LecturaClinica;
import com.redsaludandina.compartido.evento.ValorClinicoDescartado;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditoriaTest {

    private static final Instant T = Instant.parse("2026-11-20T12:00:00Z");

    private static EventoAcceso lectura(String usuario, String recurso, long segundos, String uuid) {
        return EventoAcceso.desde(new LecturaClinica(usuario, recurso, "Control", T.plusSeconds(segundos)),
                UUID.fromString(uuid));
    }

    @Test
    void elReporteDiceQuienVioUnDocumentoEnOrdenReproducible() {
        EventoAcceso tarde = lectura("esp-2", "episodio/1", 60, "00000000-0000-0000-0000-000000000001");
        EventoAcceso temprano = lectura("esp-1", "episodio/1", 0, "00000000-0000-0000-0000-000000000002");
        EventoAcceso mismoInstanteA = lectura("esp-3", "episodio/1", 30, "00000000-0000-0000-0000-00000000000a");
        EventoAcceso mismoInstanteB = lectura("esp-4", "episodio/1", 30, "00000000-0000-0000-0000-00000000000b");
        EventoAcceso otroRecurso = lectura("esp-1", "episodio/2", 10, "00000000-0000-0000-0000-000000000003");

        List<EventoAcceso> unOrden = ReporteAccesos.quienVio("episodio/1",
                List.of(tarde, otroRecurso, mismoInstanteB, temprano, mismoInstanteA));
        List<EventoAcceso> otroOrden = ReporteAccesos.quienVio("episodio/1",
                List.of(mismoInstanteA, temprano, tarde, mismoInstanteB, otroRecurso));

        assertThat(unOrden).extracting(EventoAcceso::usuarioId).containsExactly("esp-1", "esp-3", "esp-4", "esp-2");
        assertThat(otroOrden).isEqualTo(unOrden);
    }

    @Test
    void unValorDescartadoSeGuardaConAmbosValores() {
        ValorClinicoDescartado descarte = new ValorClinicoDescartado(id(1), "HALLAZGOS",
                "Tos productiva", new SelloCentro("centro-b", T.plusSeconds(10)),
                "Tos seca", new SelloCentro("centro-a", T), T.plusSeconds(20));

        EventoAcceso registro = EventoAcceso.desde(descarte, UUID.randomUUID());

        assertThat(registro.tipo()).isEqualTo(TipoAcceso.VALOR_CLINICO_DESCARTADO);
        assertThat(registro.detalle()).contains("Tos productiva").contains("Tos seca");
        assertThat(ReporteAccesos.quienVio(registro.recurso(), List.of(registro))).isEmpty();
    }
}
