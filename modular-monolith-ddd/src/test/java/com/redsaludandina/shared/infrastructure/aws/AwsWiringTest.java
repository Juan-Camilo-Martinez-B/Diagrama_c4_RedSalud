package com.redsaludandina.shared.infrastructure.aws;

import static org.assertj.core.api.Assertions.assertThat;

import com.redsaludandina.insights.application.port.AccessLogReader;
import com.redsaludandina.insights.application.port.AccessLogWriter;
import com.redsaludandina.insights.infrastructure.s3.S3ObjectLockAccessLog;
import com.redsaludandina.prescription.application.port.PharmacyGateway;
import com.redsaludandina.prescription.infrastructure.pharmacy.SqsPharmacyGateway;
import com.redsaludandina.shared.application.messaging.EventBusPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Con las propiedades del perfil aws el contexto arranca con los adaptadores de AWS y no con los
 * locales. Los clientes de AWS no se conectan al crearse, así que no hace falta red.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:redsalud-aws-wiring;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "redsalud.messaging.type=sqs",
        "redsalud.pharmacy.type=sqs",
        "redsalud.audit.type=s3",
        "redsalud.aws.endpoint=http://localhost:1",
        "redsalud.sqs.events-queue-url=http://localhost:1/000/domain-events.fifo",
        "redsalud.sqs.pharmacy-queue-url=http://localhost:1/000/pharmacy.fifo",
        "redsalud.sqs.initial-delay=PT1H",
        "redsalud.s3.audit-bucket=redsalud-audit",
        "redsalud.audit.retention=P3650D"
})
@ActiveProfiles("test")
class AwsWiringTest {

    @Autowired
    private EventBusPort bus;

    @Autowired
    private PharmacyGateway pharmacy;

    @Autowired
    private AccessLogWriter accessLogWriter;

    @Autowired
    private AccessLogReader accessLogReader;

    @Autowired
    private SqsFifoReceiver receiver;

    @Test
    void theAwsProfileUsesSqsAndS3InsteadOfTheLocalAdapters() {
        assertThat(bus).isInstanceOf(SqsFifoPublisher.class);
        assertThat(pharmacy).isInstanceOf(SqsPharmacyGateway.class);
        assertThat(accessLogWriter).isInstanceOf(S3ObjectLockAccessLog.class);
        assertThat(accessLogReader).isSameAs(accessLogWriter);
        assertThat(receiver).isNotNull();
    }
}
