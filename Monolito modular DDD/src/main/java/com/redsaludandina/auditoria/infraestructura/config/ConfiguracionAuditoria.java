package com.redsaludandina.auditoria.infraestructura.config;

import com.redsaludandina.auditoria.aplicacion.puerto.entrada.RegistrarAcceso;
import com.redsaludandina.auditoria.aplicacion.puerto.salida.RegistroAccesosPort;
import com.redsaludandina.auditoria.aplicacion.servicio.ServicioAuditoria;
import com.redsaludandina.auditoria.infraestructura.adaptador.salida.persistencia.RegistroAccesosJdbc;
import com.redsaludandina.auditoria.infraestructura.adaptador.salida.s3.RegistroAccesosS3ObjectLock;
import com.redsaludandina.compartido.infraestructura.mensajeria.ConsumidorEventos;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import software.amazon.awssdk.services.s3.S3Client;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cableado de Auditoría y de su política «Auditar acceso». El almacén se elige con
 * {@code redsalud.auditoria.tipo}: {@code s3} (S3 Object Lock, perfil aws) o {@code local} (tabla).
 */
@Configuration
public class ConfiguracionAuditoria {

    @Bean
    @ConditionalOnProperty(name = "redsalud.auditoria.tipo", havingValue = "local", matchIfMissing = true)
    RegistroAccesosPort registroAccesosLocal(JdbcClient jdbc) {
        return new RegistroAccesosJdbc(jdbc);
    }

    @Bean
    @ConditionalOnProperty(name = "redsalud.auditoria.tipo", havingValue = "s3")
    RegistroAccesosPort registroAccesosS3(S3Client s3, @Value("${redsalud.s3.bucket-auditoria}") String bucket,
                                          @Value("${redsalud.auditoria.retencion}") Duration retencion) {
        return new RegistroAccesosS3ObjectLock(s3, bucket, retencion, JsonMapper.builder().build());
    }

    @Bean
    ServicioAuditoria servicioAuditoria(RegistroAccesosPort registro) {
        return new ServicioAuditoria(registro);
    }

    @Bean
    ConsumidorEventos consumidorAuditoria(RegistrarAcceso registrarAcceso) {
        return new ConsumidorEventos("auditoria.auditar-acceso", registrarAcceso::procesar);
    }
}
