package com.redsaludandina.receta.infraestructura.config;

import com.redsaludandina.compartido.aplicacion.UnidadDeTrabajo;
import com.redsaludandina.compartido.aplicacion.mensajeria.BusEventosPort;
import com.redsaludandina.compartido.aplicacion.mensajeria.RegistradorOutbox;
import com.redsaludandina.compartido.aplicacion.mensajeria.RelevoOutbox;
import com.redsaludandina.compartido.infraestructura.mensajeria.ConsumidorEventos;
import com.redsaludandina.compartido.infraestructura.mensajeria.InboxJdbc;
import com.redsaludandina.compartido.infraestructura.mensajeria.OutboxJdbc;
import com.redsaludandina.compartido.infraestructura.mensajeria.SerializadorEventos;
import com.redsaludandina.receta.aplicacion.puerto.entrada.EnviarRecetaAFarmacia;
import com.redsaludandina.receta.aplicacion.puerto.salida.FarmaciaPort;
import com.redsaludandina.receta.aplicacion.servicio.ServicioReceta;
import com.redsaludandina.receta.infraestructura.adaptador.salida.farmacia.FarmaciaRegistroLocal;
import com.redsaludandina.receta.infraestructura.adaptador.salida.farmacia.FarmaciaSqs;
import com.redsaludandina.receta.infraestructura.adaptador.salida.persistencia.RecetaRepositorioJdbc;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cableado de Receta y de su política «Enviar a farmacia». La farmacia se elige con
 * {@code redsalud.farmacia.tipo}: {@code sqs} (cola SQS FIFO, perfil aws) o {@code local} (tabla).
 */
@Configuration
public class ConfiguracionReceta {

    static final String ESQUEMA = "receta";

    @Bean
    @ConditionalOnProperty(name = "redsalud.farmacia.tipo", havingValue = "local", matchIfMissing = true)
    FarmaciaPort farmaciaLocal(JdbcClient jdbc, Clock reloj) {
        return new FarmaciaRegistroLocal(jdbc, reloj);
    }

    @Bean
    @ConditionalOnProperty(name = "redsalud.farmacia.tipo", havingValue = "sqs")
    FarmaciaPort farmaciaSqs(SqsClient sqs, @Value("${redsalud.sqs.cola-farmacia-url}") String colaUrl) {
        return new FarmaciaSqs(sqs, colaUrl, JsonMapper.builder().build());
    }

    @Bean
    ServicioReceta servicioReceta(JdbcClient jdbc, FarmaciaPort farmacia, UnidadDeTrabajo unidadDeTrabajo,
                                  SerializadorEventos serializador, Clock reloj) {
        return new ServicioReceta(new RecetaRepositorioJdbc(jdbc), farmacia, new InboxJdbc(jdbc, ESQUEMA), unidadDeTrabajo,
                new RegistradorOutbox(ESQUEMA, new OutboxJdbc(jdbc, ESQUEMA, serializador), reloj), reloj);
    }

    @Bean
    ConsumidorEventos consumidorEnvioFarmacia(EnviarRecetaAFarmacia enviarRecetaAFarmacia) {
        return new ConsumidorEventos("receta.enviar-a-farmacia", enviarRecetaAFarmacia::procesar);
    }

    @Bean
    RelevoOutbox relevoReceta(JdbcClient jdbc, SerializadorEventos serializador, BusEventosPort bus, Clock reloj) {
        return new RelevoOutbox(new OutboxJdbc(jdbc, ESQUEMA, serializador), bus, reloj);
    }
}
