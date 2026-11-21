package com.redsaludandina.legacy.infrastructure.acl;

import com.redsaludandina.legacy.application.port.HistoryReadModelReader;
import com.redsaludandina.legacy.application.port.HistoryReadModelWriter;
import com.redsaludandina.legacy.application.port.LegacyHistorySource;
import com.redsaludandina.legacy.infrastructure.acl.adapter.LegacyHistoryAdapter;
import com.redsaludandina.legacy.infrastructure.acl.client.db.LegacyDbClient;
import com.redsaludandina.legacy.infrastructure.acl.client.soap.LegacySoapClient;
import com.redsaludandina.legacy.infrastructure.acl.job.JobStateRepository;
import com.redsaludandina.legacy.infrastructure.acl.job.NightlyMaterializationJob;
import com.redsaludandina.legacy.infrastructure.acl.readmodel.HistoryReadModelJpaRepository;
import com.redsaludandina.legacy.infrastructure.acl.readmodel.HistoryReadModelQueryRepository;
import com.redsaludandina.legacy.infrastructure.acl.readmodel.JpaHistoryReadModelReader;
import com.redsaludandina.legacy.infrastructure.acl.readmodel.JpaHistoryReadModelWriter;
import com.redsaludandina.legacy.infrastructure.acl.translator.CanonicalTranslator;
import com.zaxxer.hikari.HikariDataSource;
import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cableado de la capa anticorrupción. Hacia afuera solo publica PUERTOS del núcleo
 * ({@link LegacyHistorySource} y el modelo de lectura); sus clientes, su modelo y su traductor no
 * salen de aquí.
 */
@Configuration
public class AclConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(AclConfiguration.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Bean(destroyMethod = "close")
    LegacyDbClient legacyDbClient(@Value("${redsalud.legacy.db.url}") String url,
                                  @Value("${redsalud.legacy.db.user:}") String user,
                                  @Value("${redsalud.legacy.db.password:}") String password) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setPoolName("legacy-read-only");
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(user);
        dataSource.setPassword(password);
        dataSource.setReadOnly(true);
        dataSource.setMaximumPoolSize(5);
        dataSource.setConnectionTimeout(Duration.ofSeconds(LegacyDbClient.TIMEOUT_SECONDS).toMillis());
        return new LegacyDbClient(dataSource);
    }

    @Bean
    CanonicalTranslator canonicalTranslator() {
        return new CanonicalTranslator();
    }

    @Bean
    LegacyHistorySource legacyHistorySource(LegacyDbClient client, CanonicalTranslator translator) {
        return new LegacyHistoryAdapter(client, translator);
    }

    @Bean
    HistoryReadModelWriter historyReadModelWriter(HistoryReadModelJpaRepository jpa) {
        return new JpaHistoryReadModelWriter(jpa, JSON);
    }

    @Bean
    HistoryReadModelReader historyReadModelReader(HistoryReadModelQueryRepository queries) {
        return new JpaHistoryReadModelReader(queries, JSON);
    }

    @Bean
    NightlyJobSchedule nightlyJobSchedule(ObjectProvider<LegacySoapClient> soap, CanonicalTranslator translator,
                                          HistoryReadModelWriter readModel, JobStateRepository state, Clock clock,
                                          @Value("${redsalud.legacy.ttl:PT24H}") Duration ttl,
                                          @Value("${redsalud.legacy.job.enabled:false}") boolean enabled) {
        return new NightlyJobSchedule(soap, enabled,
                client -> new NightlyMaterializationJob(client, translator, readModel, state, clock, ttl));
    }

    /** Dispara el job nocturno si está habilitado y existe el cliente SOAP (pendiente del WSDL). */
    public static class NightlyJobSchedule {

        private final ObjectProvider<LegacySoapClient> soap;
        private final boolean enabled;
        private final java.util.function.Function<LegacySoapClient, NightlyMaterializationJob> jobFactory;

        NightlyJobSchedule(ObjectProvider<LegacySoapClient> soap, boolean enabled,
                           java.util.function.Function<LegacySoapClient, NightlyMaterializationJob> jobFactory) {
            this.soap = soap;
            this.enabled = enabled;
            this.jobFactory = jobFactory;
        }

        @Scheduled(cron = "${redsalud.legacy.job.cron:0 0 2 * * *}")
        public void run() {
            LegacySoapClient client = soap.getIfAvailable();
            if (!enabled || client == null) {
                return;
            }
            LOG.info("Job nocturno del legado: {} historiales rematerializados", jobFactory.apply(client).run());
        }
    }
}
