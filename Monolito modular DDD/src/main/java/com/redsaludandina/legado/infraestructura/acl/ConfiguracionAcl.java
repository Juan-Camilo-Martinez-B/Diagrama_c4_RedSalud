package com.redsaludandina.legado.infraestructura.acl;

import com.redsaludandina.legado.aplicacion.puerto.salida.HistorialClinicoPort;
import com.redsaludandina.legado.infraestructura.acl.adaptador.AdaptadorHistorial;
import com.redsaludandina.legado.infraestructura.acl.cliente.bd.ClienteBdLectura;
import com.redsaludandina.legado.infraestructura.acl.cliente.soap.ClienteSoapLegado;
import com.redsaludandina.legado.infraestructura.acl.job.JobMaterializacionNocturna;
import com.redsaludandina.legado.infraestructura.acl.readmodel.ReadModelHistorial;
import com.redsaludandina.legado.infraestructura.acl.traductor.TraductorCanonico;
import com.zaxxer.hikari.HikariDataSource;
import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cableado de la capa anticorrupción, DENTRO de la propia capa: hacia afuera solo se ofrece
 * {@link HistorialClinicoPort}. Ni la configuración de Integración legado ni ningún otro paquete
 * conoce los clientes ni el modelo del legado (lo vigila CapaAnticorrupcionLegadoTest).
 *
 * <p>La conexión al legado es un pool APARTE y de solo lectura que no se publica como bean de tipo
 * DataSource: así no reemplaza a la base principal ni otro contexto puede usarla.
 */
@Configuration
public class ConfiguracionAcl {

    private static final Logger LOG = LoggerFactory.getLogger(ConfiguracionAcl.class);

    @Bean(destroyMethod = "cerrar")
    ClienteBdLectura clienteBdLectura(@Value("${redsalud.legado.bd.url}") String url,
                                      @Value("${redsalud.legado.bd.usuario:}") String usuario,
                                      @Value("${redsalud.legado.bd.clave:}") String clave) {
        HikariDataSource fuente = new HikariDataSource();
        fuente.setPoolName("legado-solo-lectura");
        fuente.setJdbcUrl(url);
        fuente.setUsername(usuario);
        fuente.setPassword(clave);
        fuente.setReadOnly(true);
        fuente.setMaximumPoolSize(5);
        fuente.setConnectionTimeout(Duration.ofSeconds(ClienteBdLectura.LIMITE_SEGUNDOS).toMillis());
        return new ClienteBdLectura(fuente);
    }

    @Bean
    TraductorCanonico traductorCanonico() {
        return new TraductorCanonico();
    }

    @Bean
    ReadModelHistorial readModelHistorial(JdbcClient jdbc) {
        return new ReadModelHistorial(jdbc, JsonMapper.builder().build());
    }

    @Bean
    HistorialClinicoPort historialClinicoPort(ReadModelHistorial readModel, ClienteBdLectura clienteBd,
                                              TraductorCanonico traductor, Clock reloj,
                                              @Value("${redsalud.legado.ttl:PT24H}") Duration ttl) {
        return new AdaptadorHistorial(readModel, clienteBd, traductor, reloj, ttl);
    }

    @Bean
    ProgramacionJobNocturno programacionJobNocturno(ObjectProvider<ClienteSoapLegado> soap, TraductorCanonico traductor,
                                                    ReadModelHistorial readModel, JdbcClient jdbc, Clock reloj,
                                                    @Value("${redsalud.legado.ttl:PT24H}") Duration ttl,
                                                    @Value("${redsalud.legado.job.habilitado:false}") boolean habilitado) {
        return new ProgramacionJobNocturno(soap, traductor, readModel, jdbc, reloj, ttl, habilitado);
    }

    /**
     * Corre el job en la ventana nocturna del SOAP. No hace nada mientras no haya un
     * {@link ClienteSoapLegado} (falta el WSDL) o el job esté deshabilitado.
     */
    public static class ProgramacionJobNocturno {

        private final ObjectProvider<ClienteSoapLegado> soap;
        private final TraductorCanonico traductor;
        private final ReadModelHistorial readModel;
        private final JdbcClient jdbc;
        private final Clock reloj;
        private final Duration ttl;
        private final boolean habilitado;

        ProgramacionJobNocturno(ObjectProvider<ClienteSoapLegado> soap, TraductorCanonico traductor,
                                ReadModelHistorial readModel, JdbcClient jdbc, Clock reloj, Duration ttl, boolean habilitado) {
            this.soap = soap;
            this.traductor = traductor;
            this.readModel = readModel;
            this.jdbc = jdbc;
            this.reloj = reloj;
            this.ttl = ttl;
            this.habilitado = habilitado;
        }

        @Scheduled(cron = "${redsalud.legado.job.cron:0 0 2 * * *}")
        public void ejecutar() {
            ClienteSoapLegado cliente = soap.getIfAvailable();
            if (!habilitado || cliente == null) {
                return;
            }
            int materializados = new JobMaterializacionNocturna(cliente, traductor, readModel, jdbc, reloj, ttl).ejecutar();
            LOG.info("Job nocturno del legado: {} historiales rematerializados", materializados);
        }
    }
}
