package com.redsaludandina.legado.infraestructura.acl.job;

import com.redsaludandina.compartido.infraestructura.persistencia.Jdbc;
import com.redsaludandina.legado.dominio.modelo.MarcaFrescura;
import com.redsaludandina.legado.dominio.modelo.OrigenHistorial;
import com.redsaludandina.legado.infraestructura.acl.cliente.soap.ClienteSoapLegado;
import com.redsaludandina.legado.infraestructura.acl.modelo.HistorialLegado;
import com.redsaludandina.legado.infraestructura.acl.readmodel.ReadModelHistorial;
import com.redsaludandina.legado.infraestructura.acl.traductor.TraductorCanonico;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Rematerializa el read model con lo que el legado publica por SOAP en su ventana nocturna.
 * Recuerda la última corrida en legado.estado_job para pedir solo lo actualizado desde entonces.
 */
public final class JobMaterializacionNocturna {

    static final String NOMBRE = "materializacion-nocturna";

    private final ClienteSoapLegado soap;
    private final TraductorCanonico traductor;
    private final ReadModelHistorial readModel;
    private final JdbcClient jdbc;
    private final Clock reloj;
    private final Duration ttl;

    public JobMaterializacionNocturna(ClienteSoapLegado soap, TraductorCanonico traductor, ReadModelHistorial readModel,
                                      JdbcClient jdbc, Clock reloj, Duration ttl) {
        this.soap = soap;
        this.traductor = traductor;
        this.readModel = readModel;
        this.jdbc = jdbc;
        this.reloj = reloj;
        this.ttl = ttl;
    }

    /** Devuelve cuántos historiales rematerializó. */
    public int ejecutar() {
        Instant ahora = reloj.instant();
        Instant desde = jdbc.sql("SELECT ultima_corrida FROM legado.estado_job WHERE nombre = :nombre")
                .param("nombre", NOMBRE)
                .query((fila, n) -> Jdbc.instante(fila, "ultima_corrida"))
                .optional()
                .orElse(Instant.EPOCH);
        List<HistorialLegado> actualizados = soap.historialesActualizadosDesde(desde);
        for (HistorialLegado legado : actualizados) {
            readModel.guardar(traductor.traducir(legado, MarcaFrescura.conTtl(ahora, ttl, OrigenHistorial.READ_MODEL)));
        }
        int filas = jdbc.sql("UPDATE legado.estado_job SET ultima_corrida = :ahora WHERE nombre = :nombre")
                .param("ahora", Jdbc.aFecha(ahora)).param("nombre", NOMBRE).update();
        if (filas == 0) {
            jdbc.sql("INSERT INTO legado.estado_job (nombre, ultima_corrida) VALUES (:nombre, :ahora)")
                    .param("nombre", NOMBRE).param("ahora", Jdbc.aFecha(ahora)).update();
        }
        return actualizados.size();
    }
}
