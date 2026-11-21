package com.redsaludandina.shared.infrastructure.aws;

import com.redsaludandina.shared.application.messaging.EventBusPort;
import com.redsaludandina.shared.application.messaging.OutboxMessage;
import com.redsaludandina.shared.infrastructure.messaging.EventSerializer;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import tools.jackson.databind.json.JsonMapper;

/**
 * Recibe los eventos de la cola SQS FIFO y los entrega a los consumidores del monolito.
 *
 * <ul>
 *   <li>Solo BORRA un mensaje cuando todos los consumidores lo procesaron. Si falla, vuelve a ser
 *       visible al vencer su visibility timeout (y tras varios intentos va a la DLQ).</li>
 *   <li>Dentro de un mismo lote recibido no procesa los siguientes mensajes del grupo que falló: así
 *       se conserva el orden por agregado.</li>
 * </ul>
 */
public class SqsFifoReceiver {

    private static final Logger LOG = LoggerFactory.getLogger(SqsFifoReceiver.class);

    private final SqsClient sqs;
    private final String queueUrl;
    private final EventSerializer serializer;
    private final JsonMapper json;
    private final EventBusPort dispatcher;
    private final int waitSeconds;

    public SqsFifoReceiver(SqsClient sqs, String queueUrl, EventSerializer serializer, JsonMapper json,
                           EventBusPort dispatcher, int waitSeconds) {
        this.sqs = sqs;
        this.queueUrl = queueUrl;
        this.serializer = serializer;
        this.json = json;
        this.dispatcher = dispatcher;
        this.waitSeconds = waitSeconds;
    }

    @Scheduled(fixedDelayString = "${redsalud.sqs.receive-interval:PT1S}",
            initialDelayString = "${redsalud.sqs.initial-delay:PT5S}")
    public void scheduled() {
        receive();
    }

    /** Procesa un lote de la cola y devuelve cuántos mensajes se procesaron y borraron. */
    public int receive() {
        Set<String> stoppedGroups = new HashSet<>();
        int processed = 0;
        for (Message message : fetch()) {
            processed += process(message, stoppedGroups) ? 1 : 0;
        }
        return processed;
    }

    private Iterable<Message> fetch() {
        return sqs.receiveMessage(request -> request
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(waitSeconds)
                .messageSystemAttributeNames(MessageSystemAttributeName.MESSAGE_GROUP_ID)).messages();
    }

    private boolean process(Message message, Set<String> stoppedGroups) {
        String group = message.attributes().getOrDefault(MessageSystemAttributeName.MESSAGE_GROUP_ID, message.messageId());
        if (stoppedGroups.contains(group)) {
            return false;
        }
        try {
            dispatcher.publish(toOutboxMessage(json.readValue(message.body(), EventEnvelope.class)));
            sqs.deleteMessage(delete -> delete.queueUrl(queueUrl).receiptHandle(message.receiptHandle()));
            return true;
        } catch (RuntimeException failure) {
            stoppedGroups.add(group);
            LOG.warn("Mensaje {} sin procesar; se reintentará: {}", message.messageId(), failure.getMessage());
            return false;
        }
    }

    private OutboxMessage toOutboxMessage(EventEnvelope envelope) {
        return new OutboxMessage(envelope.eventId(), envelope.context(), envelope.aggregateId(),
                serializer.fromJson(envelope.type(), envelope.payload()), envelope.recordedAt());
    }
}
