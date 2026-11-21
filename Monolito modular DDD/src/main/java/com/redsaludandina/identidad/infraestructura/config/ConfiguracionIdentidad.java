package com.redsaludandina.identidad.infraestructura.config;

import com.redsaludandina.compartido.aplicacion.UnidadDeTrabajo;
import com.redsaludandina.compartido.aplicacion.mensajeria.BusEventosPort;
import com.redsaludandina.compartido.aplicacion.mensajeria.RegistradorOutbox;
import com.redsaludandina.compartido.aplicacion.mensajeria.RelevoOutbox;
import com.redsaludandina.compartido.infraestructura.config.ConfiguracionCompartida.TiposDeEvento;
import com.redsaludandina.compartido.infraestructura.mensajeria.OutboxJdbc;
import com.redsaludandina.compartido.infraestructura.mensajeria.SerializadorEventos;
import com.redsaludandina.identidad.aplicacion.servicio.ServicioAutorizacion;
import com.redsaludandina.identidad.dominio.evento.AccesoDenegado;
import com.redsaludandina.identidad.dominio.servicio.PoliticaAcceso;
import com.redsaludandina.identidad.infraestructura.adaptador.salida.persistencia.UsuarioRepositorioJdbc;
import java.time.Clock;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Cableado de Identidad y acceso: el OHS de autorización y su outbox (AccesoDenegado). */
@Configuration
public class ConfiguracionIdentidad {

    static final String ESQUEMA = "identidad";

    @Bean
    TiposDeEvento eventosIdentidad() {
        return new TiposDeEvento(List.of(AccesoDenegado.class));
    }

    @Bean
    ServicioAutorizacion servicioAutorizacion(JdbcClient jdbc, UnidadDeTrabajo unidadDeTrabajo,
                                              SerializadorEventos serializador, Clock reloj) {
        return new ServicioAutorizacion(new UsuarioRepositorioJdbc(jdbc), new PoliticaAcceso(), unidadDeTrabajo,
                new RegistradorOutbox(ESQUEMA, new OutboxJdbc(jdbc, ESQUEMA, serializador), reloj), reloj);
    }

    @Bean
    RelevoOutbox relevoIdentidad(JdbcClient jdbc, SerializadorEventos serializador, BusEventosPort bus, Clock reloj) {
        return new RelevoOutbox(new OutboxJdbc(jdbc, ESQUEMA, serializador), bus, reloj);
    }
}
