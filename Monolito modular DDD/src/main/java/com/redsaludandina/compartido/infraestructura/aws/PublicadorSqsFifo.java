package com.redsaludandina.compartido.infraestructura.aws;

import com.redsaludandina.compartido.aplicacion.mensajeria.BusEventosPort;
import com.redsaludandina.compartido.aplicacion.mensajeria.MensajeOutbox;
import com.redsaludandina.compartido.infraestructura.mensajeria.SerializadorEventos;
import software.amazon.awssdk.services.sqs.SqsClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Publicador del outbox hacia una cola SQS FIFO (diagrama de secuencia: «publica evento,
 * at-least-once» → «SQS, orden por agregado»).
 *
 * <ul>
 *   <li>{@code MessageGroupId} = contexto:agregado → SQS entrega en orden los eventos de cada
 *       agregado y en paralelo los de agregados distintos.</li>
 *   <li>{@code MessageDeduplicationId} = id del evento → si el relevo reenvía el mismo evento
 *       dentro de la ventana de 5 minutos, SQS lo descarta. Fuera de ella, los consumidores lo
 *       descartan por su inbox.</li>
 * </ul>
 */
public final class PublicadorSqsFifo implements BusEventosPort {

    private final SqsClient sqs;
    private final String colaUrl;
    private final SerializadorEventos serializador;
    private final JsonMapper json;

    public PublicadorSqsFifo(SqsClient sqs, String colaUrl, SerializadorEventos serializador, JsonMapper json) {
        if (colaUrl == null || !colaUrl.endsWith(".fifo")) {
            throw new IllegalArgumentException("La cola de eventos debe ser SQS FIFO (.fifo) para conservar el orden: " + colaUrl);
        }
        this.sqs = sqs;
        this.colaUrl = colaUrl;
        this.serializador = serializador;
        this.json = json;
    }

    @Override
    public void publicar(MensajeOutbox mensaje) {
        SobreEvento sobre = new SobreEvento(mensaje.eventoId(), mensaje.contexto(), mensaje.agregadoId(),
                serializador.tipo(mensaje.evento()), serializador.aJson(mensaje.evento()), mensaje.registradoEn());
        sqs.sendMessage(envio -> envio
                .queueUrl(colaUrl)
                .messageGroupId(mensaje.contexto() + ":" + mensaje.agregadoId())
                .messageDeduplicationId(mensaje.eventoId().toString())
                .messageBody(json.writeValueAsString(sobre)));
    }
}
