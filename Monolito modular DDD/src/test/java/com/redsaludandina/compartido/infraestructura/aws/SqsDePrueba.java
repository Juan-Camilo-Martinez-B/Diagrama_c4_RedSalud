package com.redsaludandina.compartido.infraestructura.aws;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

/** Cliente SQS falso: guarda lo enviado y lo borrado, y entrega los mensajes que se le cargan. */
public final class SqsDePrueba implements SqsClient {

    public final List<SendMessageRequest> enviados = new ArrayList<>();
    public final List<String> borrados = new ArrayList<>();
    public final List<ReceiveMessageRequest> recepciones = new ArrayList<>();
    public List<Message> porEntregar = List.of();

    @Override
    public SendMessageResponse sendMessage(SendMessageRequest solicitud) {
        enviados.add(solicitud);
        return SendMessageResponse.builder().messageId("m-" + enviados.size()).build();
    }

    @Override
    public ReceiveMessageResponse receiveMessage(ReceiveMessageRequest solicitud) {
        recepciones.add(solicitud);
        return ReceiveMessageResponse.builder().messages(porEntregar).build();
    }

    @Override
    public DeleteMessageResponse deleteMessage(DeleteMessageRequest solicitud) {
        borrados.add(solicitud.receiptHandle());
        return DeleteMessageResponse.builder().build();
    }

    @Override
    public String serviceName() {
        return "sqs";
    }

    @Override
    public void close() {
    }
}
