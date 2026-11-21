package com.redsaludandina.interconsulta.infraestructura.config;

import com.redsaludandina.compartido.aplicacion.UnidadDeTrabajo;
import com.redsaludandina.compartido.aplicacion.mensajeria.BusEventosPort;
import com.redsaludandina.compartido.aplicacion.mensajeria.RegistradorOutbox;
import com.redsaludandina.compartido.aplicacion.mensajeria.RelevoOutbox;
import com.redsaludandina.compartido.infraestructura.mensajeria.ConsumidorEventos;
import com.redsaludandina.compartido.infraestructura.mensajeria.InboxJdbc;
import com.redsaludandina.compartido.infraestructura.mensajeria.OutboxJdbc;
import com.redsaludandina.compartido.infraestructura.mensajeria.SerializadorEventos;
import com.redsaludandina.interconsulta.aplicacion.puerto.entrada.AvanzarInterconsulta;
import com.redsaludandina.interconsulta.aplicacion.servicio.ServicioInterconsulta;
import com.redsaludandina.interconsulta.infraestructura.adaptador.salida.persistencia.InterconsultaRepositorioJdbc;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Cableado de Interconsulta y de su política «Avanzar estado». */
@Configuration
public class ConfiguracionInterconsulta {

    static final String ESQUEMA = "interconsulta";

    @Bean
    ServicioInterconsulta servicioInterconsulta(JdbcClient jdbc, UnidadDeTrabajo unidadDeTrabajo,
                                                SerializadorEventos serializador, Clock reloj) {
        return new ServicioInterconsulta(new InterconsultaRepositorioJdbc(jdbc), new InboxJdbc(jdbc, ESQUEMA), unidadDeTrabajo,
                new RegistradorOutbox(ESQUEMA, new OutboxJdbc(jdbc, ESQUEMA, serializador), reloj), reloj);
    }

    @Bean
    ConsumidorEventos consumidorInterconsulta(AvanzarInterconsulta avanzarInterconsulta) {
        return new ConsumidorEventos("interconsulta.avanzar-estado", avanzarInterconsulta::procesar);
    }

    @Bean
    RelevoOutbox relevoInterconsulta(JdbcClient jdbc, SerializadorEventos serializador, BusEventosPort bus, Clock reloj) {
        return new RelevoOutbox(new OutboxJdbc(jdbc, ESQUEMA, serializador), bus, reloj);
    }
}
