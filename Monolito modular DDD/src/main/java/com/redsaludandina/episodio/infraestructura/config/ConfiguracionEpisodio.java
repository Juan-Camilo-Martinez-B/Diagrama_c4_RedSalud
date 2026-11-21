package com.redsaludandina.episodio.infraestructura.config;

import com.redsaludandina.compartido.aplicacion.UnidadDeTrabajo;
import com.redsaludandina.compartido.aplicacion.mensajeria.BusEventosPort;
import com.redsaludandina.compartido.aplicacion.mensajeria.RegistradorOutbox;
import com.redsaludandina.compartido.aplicacion.mensajeria.RelevoOutbox;
import com.redsaludandina.compartido.infraestructura.mensajeria.OutboxJdbc;
import com.redsaludandina.compartido.infraestructura.mensajeria.SerializadorEventos;
import com.redsaludandina.episodio.aplicacion.puerto.salida.AlmacenAdjuntosPort;
import com.redsaludandina.episodio.aplicacion.servicio.ServicioEpisodio;
import com.redsaludandina.episodio.infraestructura.adaptador.salida.almacenamiento.AlmacenAdjuntosS3;
import com.redsaludandina.episodio.infraestructura.adaptador.salida.persistencia.EpisodioRepositorioJdbc;
import com.redsaludandina.identidad.aplicacion.puerto.entrada.AutorizarAcceso;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/** Cableado de Episodio clínico. Autoriza a través del OHS de Identidad; adjuntos en S3 con KMS. */
@Configuration
public class ConfiguracionEpisodio {

    static final String ESQUEMA = "episodio";

    @Bean
    AlmacenAdjuntosPort almacenAdjuntos(S3Presigner presigner, @Value("${redsalud.s3.bucket-adjuntos}") String bucket,
                                        @Value("${redsalud.s3.kms-clave-adjuntos}") String claveKms,
                                        @Value("${redsalud.s3.vigencia-url:PT10M}") Duration vigencia) {
        return new AlmacenAdjuntosS3(presigner, bucket, claveKms, vigencia);
    }

    @Bean
    ServicioEpisodio servicioEpisodio(JdbcClient jdbc, AlmacenAdjuntosPort almacen, AutorizarAcceso autorizacion,
                                      UnidadDeTrabajo unidadDeTrabajo, SerializadorEventos serializador, Clock reloj) {
        return new ServicioEpisodio(new EpisodioRepositorioJdbc(jdbc), almacen, autorizacion, unidadDeTrabajo,
                new RegistradorOutbox(ESQUEMA, new OutboxJdbc(jdbc, ESQUEMA, serializador), reloj), reloj);
    }

    @Bean
    RelevoOutbox relevoEpisodio(JdbcClient jdbc, SerializadorEventos serializador, BusEventosPort bus, Clock reloj) {
        return new RelevoOutbox(new OutboxJdbc(jdbc, ESQUEMA, serializador), bus, reloj);
    }
}
