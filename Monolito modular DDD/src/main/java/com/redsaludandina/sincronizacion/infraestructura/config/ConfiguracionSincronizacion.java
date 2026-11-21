package com.redsaludandina.sincronizacion.infraestructura.config;

import com.redsaludandina.episodio.aplicacion.puerto.entrada.AbrirEpisodio;
import com.redsaludandina.episodio.aplicacion.puerto.entrada.AdjuntarArchivo;
import com.redsaludandina.episodio.aplicacion.puerto.entrada.CerrarTeleconsulta;
import com.redsaludandina.episodio.aplicacion.puerto.entrada.RegistrarAsistencia;
import com.redsaludandina.sincronizacion.aplicacion.servicio.ServicioSincronizacion;
import com.redsaludandina.sincronizacion.infraestructura.adaptador.salida.persistencia.RegistroIdempotenciaJdbc;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cableado del OHS de sincronización: despacha a los puertos de entrada de Episodio. Cupos y
 * recetas firmadas no se despachan: se rechazan en el borde.
 */
@Configuration
public class ConfiguracionSincronizacion {

    @Bean
    ServicioSincronizacion servicioSincronizacion(JdbcClient jdbc, Clock reloj, AbrirEpisodio abrirEpisodio,
                                                  RegistrarAsistencia registrarAsistencia, AdjuntarArchivo adjuntarArchivo,
                                                  CerrarTeleconsulta cerrarTeleconsulta) {
        return new ServicioSincronizacion(new RegistroIdempotenciaJdbc(jdbc, JsonMapper.builder().build(), reloj),
                abrirEpisodio, registrarAsistencia, adjuntarArchivo, cerrarTeleconsulta);
    }
}
