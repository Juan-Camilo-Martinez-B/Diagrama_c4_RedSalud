package com.redsaludandina.analitica.infraestructura.config;

import com.redsaludandina.analitica.aplicacion.puerto.entrada.ProyectarIndicadores;
import com.redsaludandina.analitica.aplicacion.servicio.ServicioAnalitica;
import com.redsaludandina.analitica.infraestructura.adaptador.salida.exportacion.IndicadorRepositorioJdbc;
import com.redsaludandina.compartido.aplicacion.UnidadDeTrabajo;
import com.redsaludandina.compartido.infraestructura.mensajeria.ConsumidorEventos;
import com.redsaludandina.compartido.infraestructura.mensajeria.InboxJdbc;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Cableado de Analítica de uso, con su inbox en el esquema analitica. */
@Configuration
public class ConfiguracionAnalitica {

    @Bean
    ServicioAnalitica servicioAnalitica(JdbcClient jdbc, UnidadDeTrabajo unidadDeTrabajo, Clock reloj) {
        return new ServicioAnalitica(new IndicadorRepositorioJdbc(jdbc), new InboxJdbc(jdbc, "analitica"),
                unidadDeTrabajo, reloj);
    }

    @Bean
    ConsumidorEventos consumidorAnalitica(ProyectarIndicadores proyectarIndicadores) {
        return new ConsumidorEventos("analitica.proyectar-indicadores", proyectarIndicadores::procesar);
    }
}
