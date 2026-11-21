package com.redsaludandina.compartido.infraestructura.aws;

import static com.redsaludandina.compartido.dominio.IdsDePrueba.id;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.redsaludandina.compartido.aplicacion.Fijos;
import com.redsaludandina.compartido.aplicacion.mensajeria.EventoEntrante;
import com.redsaludandina.compartido.aplicacion.mensajeria.MensajeOutbox;
import com.redsaludandina.compartido.evento.InterconsultaCerrada;
import com.redsaludandina.compartido.evento.PacienteAtendido;
import com.redsaludandina.compartido.infraestructura.mensajeria.BusEventosEnProceso;
import com.redsaludandina.compartido.infraestructura.mensajeria.ConsumidorEventos;
import com.redsaludandina.compartido.infraestructura.mensajeria.SerializadorEventos;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import tools.jackson.databind.json.JsonMapper;

/** Publicador y receptor de la cola SQS FIFO, con un cliente SQS falso (sin red). */
class BusSqsFifoTest {

    private static final String COLA = "https://sqs.region.amazonaws.com/123/eventos-dominio.fifo";

    private final SerializadorEventos serializador = new SerializadorEventos(JsonMapper.builder().build(),
            List.of(PacienteAtendido.class, InterconsultaCerrada.class));
    private final SqsDePrueba sqs = new SqsDePrueba();

    private static MensajeOutbox mensaje(int n, String agregado) {
        return new MensajeOutbox(new UUID(9, n), "episodio", agregado,
                new PacienteAtendido(id(n), "pac-1", null, "centro-a", Fijos.AHORA), Fijos.AHORA);
    }

    @Test
    void publicaConElAgregadoComoGrupoYElIdDelEventoComoDeduplicacion() {
        new PublicadorSqsFifo(sqs, COLA, serializador, JsonMapper.builder().build()).publicar(mensaje(1, "EP-1"));

        SendMessageRequest enviado = sqs.enviados.get(0);
        assertThat(enviado.queueUrl()).isEqualTo(COLA);
        assertThat(enviado.messageGroupId()).isEqualTo("episodio:EP-1");
        assertThat(enviado.messageDeduplicationId()).isEqualTo(new UUID(9, 1).toString());
        assertThat(enviado.messageBody()).contains("PacienteAtendido").contains("centro-a");
    }

    @Test
    void unaColaQueNoEsFifoSeRechazaPorqueNoConservaElOrden() {
        assertThatThrownBy(() -> new PublicadorSqsFifo(sqs, "https://sqs/cola-estandar", serializador,
                JsonMapper.builder().build())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void elReceptorEntregaYBorraSoloLoQueSeProcesoYNoAdelantaUnGrupoQueFallo() {
        // Lo que publicó el publicador es exactamente lo que recibe el receptor.
        PublicadorSqsFifo publicador = new PublicadorSqsFifo(sqs, COLA, serializador, JsonMapper.builder().build());
        publicador.publicar(mensaje(1, "EP-1"));
        publicador.publicar(mensaje(2, "EP-1"));
        publicador.publicar(mensaje(3, "EP-2"));
        List<Message> entrega = new ArrayList<>();
        for (int i = 0; i < sqs.enviados.size(); i++) {
            SendMessageRequest enviado = sqs.enviados.get(i);
            entrega.add(Message.builder().messageId("m" + i).receiptHandle("rh-" + i).body(enviado.messageBody())
                    .attributes(Map.of(MessageSystemAttributeName.MESSAGE_GROUP_ID, enviado.messageGroupId())).build());
        }
        sqs.porEntregar = entrega;

        List<UUID> procesados = new ArrayList<>();
        ConsumidorEventos consumidor = new ConsumidorEventos("prueba", (EventoEntrante entrante) -> {
            if (entrante.eventoId().equals(new UUID(9, 1))) {
                throw new IllegalStateException("falla transitoria");
            }
            procesados.add(entrante.eventoId());
        });
        ReceptorSqsFifo receptor = new ReceptorSqsFifo(sqs, COLA, serializador, JsonMapper.builder().build(),
                new BusEventosEnProceso(List.of(consumidor)), 0);

        int borrados = receptor.recibir();

        assertThat(borrados).isEqualTo(1);
        assertThat(procesados).as("el 2 es del mismo grupo que el 1, que falló: no se adelanta")
                .containsExactly(new UUID(9, 3));
        assertThat(sqs.borrados).containsExactly("rh-2");
        assertThat(sqs.recepciones.get(0).messageSystemAttributeNames())
                .contains(MessageSystemAttributeName.MESSAGE_GROUP_ID);
    }
}
