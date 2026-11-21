package com.redsaludandina.compartido.infraestructura.aws;

import com.redsaludandina.compartido.aplicacion.mensajeria.BusEventosPort;
import com.redsaludandina.compartido.infraestructura.mensajeria.BusEventosEnProceso;
import com.redsaludandina.compartido.infraestructura.mensajeria.ConsumidorEventos;
import com.redsaludandina.compartido.infraestructura.mensajeria.SerializadorEventos;
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
 * Clientes de AWS en la ÚNICA región del despliegue (residencia del dato, ADR-05).
 *
 * <ul>
 *   <li>Credenciales: la cadena por defecto de AWS (rol de la tarea/instancia). Con
 *       {@code redsalud.aws.credenciales-locales=true} se usan credenciales ficticias, solo para
 *       firmar URLs en el perfil local o contra un emulador.</li>
 *   <li>{@code redsalud.aws.endpoint}: opcional, para un emulador de S3/SQS en desarrollo.</li>
 * </ul>
 */
@Configuration
public class ConfiguracionAws {

    private final Region region;
    private final URI endpoint;
    private final AwsCredentialsProvider credenciales;

    public ConfiguracionAws(@Value("${redsalud.aws.region}") String region,
                            @Value("${redsalud.aws.endpoint:}") String endpoint,
                            @Value("${redsalud.aws.credenciales-locales:false}") boolean credencialesLocales) {
        this.region = Region.of(region);
        this.endpoint = endpoint.isBlank() ? null : URI.create(endpoint);
        this.credenciales = credencialesLocales
                ? StaticCredentialsProvider.create(AwsBasicCredentials.create("local", "local"))
                : DefaultCredentialsProvider.builder().build();
    }

    @Bean(destroyMethod = "close")
    S3Presigner s3Presigner() {
        S3Presigner.Builder constructor = S3Presigner.builder().region(region).credentialsProvider(credenciales)
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(endpoint != null).build());
        if (endpoint != null) {
            constructor.endpointOverride(endpoint);
        }
        return constructor.build();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "redsalud.auditoria.tipo", havingValue = "s3")
    S3Client s3Client() {
        var constructor = S3Client.builder().region(region).credentialsProvider(credenciales)
                .forcePathStyle(endpoint != null);
        if (endpoint != null) {
            constructor.endpointOverride(endpoint);
        }
        return constructor.build();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnExpression("'${redsalud.mensajeria.tipo:local}' == 'sqs' or '${redsalud.farmacia.tipo:local}' == 'sqs'")
    SqsClient sqsClient() {
        var constructor = SqsClient.builder().region(region).credentialsProvider(credenciales);
        if (endpoint != null) {
            constructor.endpointOverride(endpoint);
        }
        return constructor.build();
    }

    @Bean
    @ConditionalOnProperty(name = "redsalud.mensajeria.tipo", havingValue = "sqs")
    BusEventosPort publicadorSqsFifo(SqsClient sqs, SerializadorEventos serializador,
                                     @Value("${redsalud.sqs.cola-eventos-url}") String colaUrl) {
        return new PublicadorSqsFifo(sqs, colaUrl, serializador, JsonMapper.builder().build());
    }

    @Bean
    @ConditionalOnProperty(name = "redsalud.mensajeria.tipo", havingValue = "sqs")
    ReceptorSqsFifo receptorSqsFifo(SqsClient sqs, SerializadorEventos serializador, List<ConsumidorEventos> consumidores,
                                    @Value("${redsalud.sqs.cola-eventos-url}") String colaUrl,
                                    @Value("${redsalud.sqs.espera-segundos:10}") int esperaSegundos) {
        return new ReceptorSqsFifo(sqs, colaUrl, serializador, JsonMapper.builder().build(),
                new BusEventosEnProceso(consumidores), esperaSegundos);
    }
}
