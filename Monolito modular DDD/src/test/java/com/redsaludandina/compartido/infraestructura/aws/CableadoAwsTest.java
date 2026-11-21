package com.redsaludandina.compartido.infraestructura.aws;

import static org.assertj.core.api.Assertions.assertThat;

import com.redsaludandina.auditoria.aplicacion.puerto.salida.RegistroAccesosPort;
import com.redsaludandina.auditoria.infraestructura.adaptador.salida.s3.RegistroAccesosS3ObjectLock;
import com.redsaludandina.compartido.aplicacion.mensajeria.BusEventosPort;
import com.redsaludandina.receta.aplicacion.puerto.salida.FarmaciaPort;
import com.redsaludandina.receta.infraestructura.adaptador.salida.farmacia.FarmaciaSqs;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Con la configuración del perfil aws, el contexto arranca con los adaptadores de AWS y no con los
 * locales. Los clientes de AWS no se conectan al crearse, así que no hace falta red.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:redsalud-cableado-aws;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "redsalud.mensajeria.tipo=sqs",
        "redsalud.farmacia.tipo=sqs",
        "redsalud.auditoria.tipo=s3",
        "redsalud.aws.endpoint=http://localhost:1",
        "redsalud.sqs.cola-eventos-url=http://localhost:1/000/eventos-dominio.fifo",
        "redsalud.sqs.cola-farmacia-url=http://localhost:1/000/farmacia.fifo",
        "redsalud.sqs.retraso-inicial=PT1H",
        "redsalud.s3.bucket-auditoria=redsalud-auditoria",
        "redsalud.auditoria.retencion=P3650D"
})
@ActiveProfiles("test")
class CableadoAwsTest {

    @Autowired
    private BusEventosPort bus;

    @Autowired
    private FarmaciaPort farmacia;

    @Autowired
    private RegistroAccesosPort registroAccesos;

    @Autowired
    private ReceptorSqsFifo receptor;

    @Test
    void conElPerfilAwsSeUsanSqsYS3EnVezDeLosAdaptadoresLocales() {
        assertThat(bus).isInstanceOf(PublicadorSqsFifo.class);
        assertThat(farmacia).isInstanceOf(FarmaciaSqs.class);
        assertThat(registroAccesos).isInstanceOf(RegistroAccesosS3ObjectLock.class);
        assertThat(receptor).isNotNull();
    }
}
