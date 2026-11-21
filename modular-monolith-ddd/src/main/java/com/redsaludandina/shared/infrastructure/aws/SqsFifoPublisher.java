package com.redsaludandina.shared.infrastructure.aws;

import com.redsaludandina.shared.application.messaging.EventBusPort;
import com.redsaludandina.shared.application.messaging.OutboxMessage;
import com.redsaludandina.shared.infrastructure.messaging.EventSerializer;
import software.amazon.awssdk.services.sqs.SqsClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Publica los eventos del outbox en una cola SQS FIFO. El grupo es {@code contexto:agregado}, así SQS
 * entrega en orden los eventos de cada agregado; la deduplicación usa el id del evento.
 */
public final class SqsFifoPublisher implements EventBusPort {

    private final SqsClient sqs;
    private final String queueUrl;
    private final EventSerializer serializer;
    private final JsonMapper json;

    public SqsFifoPublisher(SqsClient sqs, String queueUrl, EventSerializer serializer, JsonMapper json) {
        if (queueUrl == null || !queueUrl.endsWith(".fifo")) {
            throw new IllegalArgumentException("La cola de eventos debe ser SQS FIFO (.fifo) para conservar el orden: " + queueUrl);
        }
        this.sqs = sqs;
        this.queueUrl = queueUrl;
        this.serializer = serializer;
        this.json = json;
    }

    @Override
    public void publish(OutboxMessage message) {
        EventEnvelope envelope = new EventEnvelope(message.eventId(), message.context(), message.aggregateId(),
                serializer.type(message.event()), serializer.toJson(message.event()), message.recordedAt());
        sqs.sendMessage(send -> send
                .queueUrl(queueUrl)
                .messageGroupId(message.context() + ":" + message.aggregateId())
                .messageDeduplicationId(message.eventId().toString())
                .messageBody(json.writeValueAsString(envelope)));
    }
}
