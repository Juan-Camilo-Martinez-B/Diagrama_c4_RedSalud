package com.redsaludandina.agenda.infraestructura.config;

import com.redsaludandina.agenda.aplicacion.servicio.ServicioAgenda;
import com.redsaludandina.agenda.infraestructura.adaptador.salida.persistencia.CupoRepositorioJdbc;
import com.redsaludandina.compartido.aplicacion.UnidadDeTrabajo;
import com.redsaludandina.compartido.aplicacion.mensajeria.BusEventosPort;
import com.redsaludandina.compartido.aplicacion.mensajeria.RegistradorOutbox;
import com.redsaludandina.compartido.aplicacion.mensajeria.RelevoOutbox;
import com.redsaludandina.compartido.infraestructura.mensajeria.OutboxJdbc;
import com.redsaludandina.compartido.infraestructura.mensajeria.SerializadorEventos;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Cableado del contexto Agenda: casos de uso, repositorio y outbox del esquema agenda. */
@Configuration
public class ConfiguracionAgenda {

    static final String ESQUEMA = "agenda";

    @Bean
    ServicioAgenda servicioAgenda(JdbcClient jdbc, UnidadDeTrabajo unidadDeTrabajo, SerializadorEventos serializador,
                                  Clock reloj) {
        return new ServicioAgenda(new CupoRepositorioJdbc(jdbc), unidadDeTrabajo,
                new RegistradorOutbox(ESQUEMA, new OutboxJdbc(jdbc, ESQUEMA, serializador), reloj), reloj);
    }

    @Bean
    RelevoOutbox relevoAgenda(JdbcClient jdbc, SerializadorEventos serializador, BusEventosPort bus, Clock reloj) {
        return new RelevoOutbox(new OutboxJdbc(jdbc, ESQUEMA, serializador), bus, reloj);
    }
}
