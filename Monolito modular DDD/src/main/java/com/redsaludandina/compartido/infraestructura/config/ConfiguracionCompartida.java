package com.redsaludandina.compartido.infraestructura.config;

import com.redsaludandina.compartido.aplicacion.UnidadDeTrabajo;
import com.redsaludandina.compartido.aplicacion.mensajeria.BusEventosPort;
import com.redsaludandina.compartido.aplicacion.mensajeria.RelevoOutbox;
import com.redsaludandina.compartido.dominio.EventoDominio;
import com.redsaludandina.compartido.evento.CupoReservado;
import com.redsaludandina.compartido.evento.InterconsultaCerrada;
import com.redsaludandina.compartido.evento.InterconsultaSolicitada;
import com.redsaludandina.compartido.evento.LecturaClinica;
import com.redsaludandina.compartido.evento.PacienteAtendido;
import com.redsaludandina.compartido.evento.RecetaFirmada;
import com.redsaludandina.compartido.evento.ValorClinicoDescartado;
import com.redsaludandina.compartido.infraestructura.mensajeria.BusEventosEnProceso;
import com.redsaludandina.compartido.infraestructura.mensajeria.ConsumidorEventos;
import com.redsaludandina.compartido.infraestructura.mensajeria.RelevoProgramado;
import com.redsaludandina.compartido.infraestructura.mensajeria.SerializadorEventos;
import com.redsaludandina.compartido.infraestructura.persistencia.UnidadDeTrabajoSpring;
import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * Piezas comunes: reloj, transacción, serialización del lenguaje publicado, bus y relevo.
 * Cada contexto aporta sus tipos de evento ({@link TiposDeEvento}), sus consumidores
 * ({@link ConsumidorEventos}) y el relevo de su outbox ({@link RelevoOutbox}).
 */
@Configuration
@EnableScheduling
public class ConfiguracionCompartida {

    /** Tipos de evento que un contexto publica en su outbox. */
    public record TiposDeEvento(List<Class<? extends EventoDominio>> tipos) {
    }

    @Bean
    Clock reloj(@Value("${redsalud.zona-horaria:UTC}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }

    @Bean
    UnidadDeTrabajo unidadDeTrabajo(PlatformTransactionManager transacciones) {
        return new UnidadDeTrabajoSpring(new TransactionTemplate(transacciones));
    }

    @Bean
    TiposDeEvento lenguajePublicado() {
        return new TiposDeEvento(List.of(CupoReservado.class, PacienteAtendido.class, RecetaFirmada.class,
                LecturaClinica.class, ValorClinicoDescartado.class, InterconsultaSolicitada.class,
                InterconsultaCerrada.class));
    }

    @Bean
    SerializadorEventos serializadorEventos(List<TiposDeEvento> aportes) {
        List<Class<? extends EventoDominio>> tipos = new ArrayList<>();
        aportes.forEach(aporte -> tipos.addAll(aporte.tipos()));
        return new SerializadorEventos(JsonMapper.builder().build(), tipos);
    }

    /** Sin AWS: el bus entrega en proceso. Con {@code redsalud.mensajeria.tipo=sqs}, lo reemplaza SQS FIFO. */
    @Bean
    @ConditionalOnProperty(name = "redsalud.mensajeria.tipo", havingValue = "local", matchIfMissing = true)
    BusEventosPort busEventos(List<ConsumidorEventos> consumidores) {
        return new BusEventosEnProceso(consumidores);
    }

    @Bean
    RelevoProgramado relevoProgramado(List<RelevoOutbox> relevos,
                                      @Value("${redsalud.outbox.relevo-automatico:true}") boolean automatico,
                                      @Value("${redsalud.outbox.lote:100}") int lote) {
        return new RelevoProgramado(relevos, automatico, lote);
    }
}
