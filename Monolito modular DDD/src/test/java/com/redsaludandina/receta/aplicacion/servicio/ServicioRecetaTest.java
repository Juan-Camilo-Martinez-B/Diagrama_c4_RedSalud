package com.redsaludandina.receta.aplicacion.servicio;

import static com.redsaludandina.compartido.dominio.IdsDePrueba.id;
import static org.assertj.core.api.Assertions.assertThat;

import com.redsaludandina.compartido.aplicacion.AlmacenEnMemoria;
import com.redsaludandina.compartido.aplicacion.Fijos;
import com.redsaludandina.compartido.aplicacion.UnidadDeTrabajoDirecta;
import com.redsaludandina.compartido.aplicacion.mensajeria.EventoEntrante;
import com.redsaludandina.compartido.aplicacion.mensajeria.InboxEnMemoria;
import com.redsaludandina.compartido.aplicacion.mensajeria.MensajeOutbox;
import com.redsaludandina.compartido.aplicacion.mensajeria.OutboxEnMemoria;
import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.compartido.evento.RecetaFirmada;
import com.redsaludandina.receta.aplicacion.puerto.entrada.AgregarMedicamento;
import com.redsaludandina.receta.aplicacion.puerto.entrada.FirmarReceta;
import com.redsaludandina.receta.aplicacion.puerto.entrada.RedactarReceta;
import com.redsaludandina.receta.aplicacion.puerto.salida.RecetaRepositorio;
import com.redsaludandina.receta.dominio.modelo.Receta;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ServicioRecetaTest {

    private final AlmacenEnMemoria<IdClinico, Receta> almacen = new AlmacenEnMemoria<>(Receta::id);
    private final OutboxEnMemoria outbox = new OutboxEnMemoria();
    private final List<IdClinico> enviadasAFarmacia = new ArrayList<>();

    private final ServicioReceta servicio = new ServicioReceta(new RecetaRepositorio() {
        @Override
        public Optional<Receta> buscar(IdClinico recetaId) {
            return almacen.buscar(recetaId);
        }

        @Override
        public void guardar(Receta receta) {
            almacen.guardar(receta);
        }
    }, receta -> enviadasAFarmacia.add(receta.id()), new InboxEnMemoria(), new UnidadDeTrabajoDirecta(),
            outbox.registrador("receta"),
            Fijos.RELOJ);

    private MensajeOutbox redactarYFirmar() {
        servicio.redactar(new RedactarReceta.Comando(id(1), id(2), "esp-1"));
        servicio.agregar(new AgregarMedicamento.Comando(id(1), "Amoxicilina", "500 mg", "Cada 8 horas"));
        servicio.firmar(new FirmarReceta.Comando(id(1), "esp-1", "firma"));
        return outbox.mensajes().get(0);
    }

    @Test
    void firmarDejaRecetaFirmadaEnElOutboxSinEnviarTodaviaALaFarmacia() {
        MensajeOutbox mensaje = redactarYFirmar();

        assertThat(mensaje.evento()).isInstanceOf(RecetaFirmada.class);
        assertThat(enviadasAFarmacia).isEmpty();
    }

    @Test
    void alConsumirRecetaFirmadaEnviaALaFarmaciaUnaSolaVezAunqueElEventoLlegueRepetido() {
        MensajeOutbox mensaje = redactarYFirmar();
        EventoEntrante entrante = new EventoEntrante(mensaje.eventoId(), mensaje.evento());

        servicio.procesar(entrante);
        servicio.procesar(entrante);

        assertThat(enviadasAFarmacia).containsExactly(id(1));
        assertThat(almacen.buscar(id(1))).get()
                .satisfies(receta -> assertThat(receta.enviadaAFarmaciaEn()).contains(Fijos.AHORA));
    }
}
