package com.redsaludandina.receta.infraestructura.adaptador.salida.farmacia;

import com.redsaludandina.compartido.infraestructura.persistencia.Jdbc;
import com.redsaludandina.receta.aplicacion.puerto.salida.FarmaciaPort;
import com.redsaludandina.receta.dominio.modelo.Receta;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Envío a farmacia PARA EL PERFIL SIN AWS: deja constancia en receta.envio_farmacia en vez de
 * publicar en la cola SQS. Descarta reenvíos por id de receta, como debe hacerlo la farmacia real.
 */
public final class FarmaciaRegistroLocal implements FarmaciaPort {

    private static final Logger LOG = LoggerFactory.getLogger(FarmaciaRegistroLocal.class);

    private final JdbcClient jdbc;
    private final Clock reloj;

    public FarmaciaRegistroLocal(JdbcClient jdbc, Clock reloj) {
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    @Override
    public void enviar(Receta receta) {
        Integer existe = jdbc.sql("SELECT COUNT(*) FROM receta.envio_farmacia WHERE receta_id = :id")
                .param("id", receta.id().valor()).query(Integer.class).single();
        if (existe == 0) {
            jdbc.sql("INSERT INTO receta.envio_farmacia (receta_id, enviado_en) VALUES (:id, :en)")
                    .param("id", receta.id().valor())
                    .param("en", Jdbc.aFecha(reloj.instant()))
                    .update();
            LOG.info("Receta {} enviada a la farmacia (registro local)", receta.id());
        }
    }
}
