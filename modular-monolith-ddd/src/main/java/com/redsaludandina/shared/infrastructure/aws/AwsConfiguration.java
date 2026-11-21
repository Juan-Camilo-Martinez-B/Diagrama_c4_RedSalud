package com.redsaludandina.shared.infrastructure.aws;

import com.redsaludandina.shared.application.messaging.EventBusPort;
import com.redsaludandina.shared.application.messaging.EventConsumer;
import com.redsaludandina.shared.infrastructure.messaging.EventSerializer;
import com.redsaludandina.shared.infrastructure.messaging.InProcessEventBus;
import java.net.URI;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sqs.SqsClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Clientes de AWS en la ÚNICA región del despliegue (ADR-05). {@code redsalud.aws.endpoint} apunta a
 * un emulador si se quiere probar sin AWS; las credenciales locales son ficticias.
 */
@Configuration
public class AwsConfiguration {

    private final Region region;
    private final URI endpoint;
    private final AwsCredentialsProvider credentials;

    public AwsConfiguration(@Value("${redsalud.aws.region}") String region,
                            @Value("${redsalud.aws.endpoint:}") String endpoint,
                            @Value("${redsalud.aws.local-credentials:false}") boolean localCredentials) {
        this.region = Region.of(region);
        this.endpoint = endpoint.isBlank() ? null : URI.create(endpoint);
        this.credentials = localCredentials
                ? StaticCredentialsProvider.create(AwsBasicCredentials.create("local", "local"))
                : DefaultCredentialsProvider.builder().build();
    }

    @Bean(destroyMethod = "close")
    S3Presigner s3Presigner() {
        S3Presigner.Builder builder = S3Presigner.builder().region(region).credentialsProvider(credentials)
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(endpoint != null).build());
        if (endpoint != null) {
            builder.endpointOverride(endpoint);
        }
        return builder.build();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "redsalud.audit.type", havingValue = "s3")
    S3Client s3Client() {
        var builder = S3Client.builder().region(region).credentialsProvider(credentials).forcePathStyle(endpoint != null);
        if (endpoint != null) {
            builder.endpointOverride(endpoint);
        }
        return builder.build();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnExpression("'${redsalud.messaging.type:local}' == 'sqs' or '${redsalud.pharmacy.type:local}' == 'sqs'")
    SqsClient sqsClient() {
        var builder = SqsClient.builder().region(region).credentialsProvider(credentials);
        if (endpoint != null) {
            builder.endpointOverride(endpoint);
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(name = "redsalud.messaging.type", havingValue = "sqs")
    EventBusPort sqsFifoPublisher(SqsClient sqs, EventSerializer serializer,
                                  @Value("${redsalud.sqs.events-queue-url}") String queueUrl) {
        return new SqsFifoPublisher(sqs, queueUrl, serializer, JsonMapper.builder().build());
    }

    @Bean
    @ConditionalOnProperty(name = "redsalud.messaging.type", havingValue = "sqs")
    SqsFifoReceiver sqsFifoReceiver(SqsClient sqs, EventSerializer serializer, List<EventConsumer<?>> consumers,
                                    @Value("${redsalud.sqs.events-queue-url}") String queueUrl,
                                    @Value("${redsalud.sqs.wait-seconds:10}") int waitSeconds) {
        return new SqsFifoReceiver(sqs, queueUrl, serializer, JsonMapper.builder().build(),
                new InProcessEventBus(consumers), waitSeconds);
    }
}
