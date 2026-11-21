package com.redsaludandina.analitica.aplicacion.servicio;

import static com.redsaludandina.compartido.dominio.IdsDePrueba.id;
import static org.assertj.core.api.Assertions.assertThat;

import com.redsaludandina.analitica.aplicacion.puerto.salida.IndicadorRepositorio;
import com.redsaludandina.analitica.dominio.modelo.IndicadorUso;
import com.redsaludandina.compartido.aplicacion.Fijos;
import com.redsaludandina.compartido.aplicacion.UnidadDeTrabajoDirecta;
import com.redsaludandina.compartido.aplicacion.mensajeria.EventoEntrante;
import com.redsaludandina.compartido.aplicacion.mensajeria.InboxEnMemoria;
import com.redsaludandina.compartido.evento.LecturaClinica;
import com.redsaludandina.compartido.evento.PacienteAtendido;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ServicioAnaliticaTest {

    private final Map<String, IndicadorUso> almacen = new HashMap<>();
    private final ServicioAnalitica servicio = new ServicioAnalitica(new IndicadorRepositorio() {
        @Override
        public Optional<IndicadorUso> buscar(String centroId, YearMonth periodo) {
            return Optional.ofNullable(almacen.get(centroId + "/" + periodo));
        }

        @Override
        public void guardar(IndicadorUso indicador) {
            almacen.put(indicador.centroId() + "/" + indicador.periodo(), indicador);
        }
    }, new InboxEnMemoria(), new UnidadDeTrabajoDirecta(), Fijos.RELOJ);

    private static EventoEntrante atencion(int n) {
        return new EventoEntrante(new UUID(2, n), new PacienteAtendido(id(n), "pac-1", null, "centro-a", Fijos.AHORA));
    }

    @Test
    void unEventoRepetidoNoSeCuentaDosVeces() {
        servicio.procesar(atencion(1));
        servicio.procesar(atencion(1));
        servicio.procesar(atencion(2));

        assertThat(servicio.consultar("centro-a", YearMonth.of(2026, 11))).get()
                .satisfies(indicador -> assertThat(indicador.teleconsultas()).isEqualTo(2));
    }

    @Test
    void losEventosQueNoMidenNadaNoCreanIndicadores() {
        servicio.procesar(new EventoEntrante(new UUID(2, 9),
                new LecturaClinica("esp-1", "historial/pac-1", "Control", Fijos.AHORA)));

        assertThat(almacen).isEmpty();
    }
}
