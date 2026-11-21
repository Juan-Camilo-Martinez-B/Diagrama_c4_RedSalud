package com.redsaludandina.legado.infraestructura.config;

import com.redsaludandina.compartido.aplicacion.UnidadDeTrabajo;
import com.redsaludandina.compartido.aplicacion.mensajeria.BusEventosPort;
import com.redsaludandina.compartido.aplicacion.mensajeria.RegistradorOutbox;
import com.redsaludandina.compartido.aplicacion.mensajeria.RelevoOutbox;
import com.redsaludandina.compartido.infraestructura.mensajeria.OutboxJdbc;
import com.redsaludandina.compartido.infraestructura.mensajeria.SerializadorEventos;
import com.redsaludandina.identidad.aplicacion.puerto.entrada.AutorizarAcceso;
import com.redsaludandina.legado.aplicacion.puerto.salida.HistorialClinicoPort;
import com.redsaludandina.legado.aplicacion.servicio.ServicioHistorialClinico;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Cableado de Integración legado. Recibe el historial solo a través de {@link HistorialClinicoPort};
 * la capa anticorrupción se cablea a sí misma en {@code legado.infraestructura.acl.ConfiguracionAcl}.
 */
@Configuration
public class ConfiguracionLegado {

    static final String ESQUEMA = "legado";

    @Bean
    ServicioHistorialClinico servicioHistorialClinico(AutorizarAcceso autorizacion, HistorialClinicoPort historiales,
                                                      JdbcClient jdbc, UnidadDeTrabajo unidadDeTrabajo,
                                                      SerializadorEventos serializador, Clock reloj) {
        return new ServicioHistorialClinico(autorizacion, historiales, unidadDeTrabajo,
                new RegistradorOutbox(ESQUEMA, new OutboxJdbc(jdbc, ESQUEMA, serializador), reloj), reloj);
    }

    @Bean
    RelevoOutbox relevoLegado(JdbcClient jdbc, SerializadorEventos serializador, BusEventosPort bus, Clock reloj) {
        return new RelevoOutbox(new OutboxJdbc(jdbc, ESQUEMA, serializador), bus, reloj);
    }
}
