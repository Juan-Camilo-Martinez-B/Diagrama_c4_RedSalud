package com.redsaludandina.compartido.aplicacion.mensajeria;

import static com.redsaludandina.compartido.dominio.IdsDePrueba.id;
import static org.assertj.core.api.Assertions.assertThat;

import com.redsaludandina.compartido.aplicacion.Fijos;
import com.redsaludandina.compartido.aplicacion.mensajeria.RelevoOutbox.ResultadoRelevo;
import com.redsaludandina.compartido.evento.InterconsultaCerrada;
import com.redsaludandina.compartido.evento.InterconsultaSolicitada;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RelevoOutboxTest {

    private final OutboxEnMemoria outbox = new OutboxEnMemoria();
    private final RegistradorOutbox registrador = outbox.registrador("interconsulta");

    /** Bus que guarda lo publicado y falla para los agregados indicados. */
    private static final class BusDePrueba implements BusEventosPort {
        final List<MensajeOutbox> publicados = new ArrayList<>();
        Set<String> agregadosQueFallan = Set.of();

        @Override
        public void publicar(MensajeOutbox mensaje) {
            if (agregadosQueFallan.contains(mensaje.agregadoId())) {
                throw new IllegalStateException("EventBridge no disponible");
            }
            publicados.add(mensaje);
        }
    }

    private void registrarDosEventosPorAgregado() {
        registrador.registrar("A", List.of(new InterconsultaSolicitada(id(1), "centro-a", "Cardiología", Fijos.AHORA),
                new InterconsultaCerrada(id(1), "centro-a", Fijos.AHORA)));
        registrador.registrar("B", List.of(new InterconsultaSolicitada(id(2), "centro-a", "Neumología", Fijos.AHORA),
                new InterconsultaCerrada(id(2), "centro-a", Fijos.AHORA)));
    }

    @Test
    void publicaLosPendientesEnOrdenYLosMarca() {
        registrarDosEventosPorAgregado();
        BusDePrueba bus = new BusDePrueba();

        ResultadoRelevo resultado = new RelevoOutbox(outbox, bus, Fijos.RELOJ).relevar(10);

        assertThat(resultado).isEqualTo(new ResultadoRelevo(4, 0, 0));
        assertThat(bus.publicados).isEqualTo(outbox.mensajes());
        assertThat(outbox.pendientes(10)).isEmpty();
    }

    @Test
    void siFallaUnAgregadoNoAdelantaSusEventosPeroSiguePublicandoLosDeOtros() {
        registrarDosEventosPorAgregado();
        BusDePrueba bus = new BusDePrueba();
        bus.agregadosQueFallan = Set.of("A");

        ResultadoRelevo resultado = new RelevoOutbox(outbox, bus, Fijos.RELOJ).relevar(10);

        assertThat(resultado).isEqualTo(new ResultadoRelevo(2, 1, 1));
        assertThat(bus.publicados).extracting(MensajeOutbox::agregadoId).containsExactly("B", "B");
        assertThat(outbox.pendientes(10)).extracting(MensajeOutbox::agregadoId).containsExactly("A", "A");

        bus.agregadosQueFallan = Set.of();
        new RelevoOutbox(outbox, bus, Fijos.RELOJ).relevar(10);

        assertThat(bus.publicados).extracting(MensajeOutbox::tipo).endsWith("InterconsultaSolicitada", "InterconsultaCerrada");
        assertThat(outbox.pendientes(10)).isEmpty();
    }
}
