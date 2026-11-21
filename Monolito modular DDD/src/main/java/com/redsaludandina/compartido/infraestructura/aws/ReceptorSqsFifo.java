package com.redsaludandina.compartido.infraestructura.aws;

import com.redsaludandina.compartido.aplicacion.mensajeria.BusEventosPort;
import com.redsaludandina.compartido.aplicacion.mensajeria.MensajeOutbox;
import com.redsaludandina.compartido.infraestructura.mensajeria.SerializadorEventos;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Recibe los eventos de la cola SQS FIFO y los entrega a los consumidores del monolito (diagrama:
 * «Q entrega → consumidor idempotente»).
 *
 * <ul>
 *   <li>Solo BORRA un mensaje cuando todos los consumidores lo procesaron. Si falla, vuelve a ser
 *       visible al vencer su visibility timeout (y tras varios intentos va a la DLQ de la cola).</li>
 *   <li>Mientras un mensaje de un grupo está pendiente, SQS FIFO no entrega los siguientes de ese
 *       grupo; dentro de un mismo lote recibido, este receptor tampoco procesa los siguientes del
 *       grupo que falló. Así se conserva el orden por agregado.</li>
 * </ul>
 */
public class ReceptorSqsFifo {

    private static final Logger LOG = LoggerFactory.getLogger(ReceptorSqsFifo.class);

    private final SqsClient sqs;
    private final String colaUrl;
    private final SerializadorEventos serializador;
    private final JsonMapper json;
    private final BusEventosPort despachador;
    private final int esperaSegundos;

    public ReceptorSqsFifo(SqsClient sqs, String colaUrl, SerializadorEventos serializador, JsonMapper json,
                           BusEventosPort despachador, int esperaSegundos) {
        this.sqs = sqs;
        this.colaUrl = colaUrl;
        this.serializador = serializador;
        this.json = json;
        this.despachador = despachador;
        this.esperaSegundos = esperaSegundos;
    }

    @Scheduled(fixedDelayString = "${redsalud.sqs.intervalo-recepcion:PT1S}",
            initialDelayString = "${redsalud.sqs.retraso-inicial:PT5S}")
    public void programado() {
        recibir();
    }

    /** Procesa un lote de la cola y devuelve cuántos mensajes se procesaron y borraron. */
    public int recibir() {
        ReceiveMessageResponse respuesta = sqs.receiveMessage(recepcion -> recepcion
                .queueUrl(colaUrl)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(esperaSegundos)
                .messageSystemAttributeNames(MessageSystemAttributeName.MESSAGE_GROUP_ID));
        Set<String> gruposDetenidos = new HashSet<>();
        int procesados = 0;
        for (Message mensaje : respuesta.messages()) {
            String grupo = mensaje.attributes().get(MessageSystemAttributeName.MESSAGE_GROUP_ID);
            if (grupo != null && gruposDetenidos.contains(grupo)) {
                continue;
            }
            try {
                SobreEvento sobre = json.readValue(mensaje.body(), SobreEvento.class);
                despachador.publicar(new MensajeOutbox(sobre.eventoId(), sobre.contexto(), sobre.agregadoId(),
                        serializador.desdeJson(sobre.tipo(), sobre.payload()), sobre.registradoEn()));
                sqs.deleteMessage(borrado -> borrado.queueUrl(colaUrl).receiptHandle(mensaje.receiptHandle()));
                procesados++;
            } catch (RuntimeException error) {
                if (grupo != null) {
                    gruposDetenidos.add(grupo);
                }
                LOG.warn("Mensaje {} sin procesar; se reintentará: {}", mensaje.messageId(), error.getMessage());
            }
        }
        return procesados;
    }
}
