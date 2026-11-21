package com.redsaludandina.legado.infraestructura.acl.readmodel;

import com.redsaludandina.compartido.infraestructura.persistencia.Jdbc;
import com.redsaludandina.legado.dominio.modelo.DocumentoDisponible;
import com.redsaludandina.legado.dominio.modelo.EpisodioHistorico;
import com.redsaludandina.legado.dominio.modelo.HistorialClinico;
import com.redsaludandina.legado.dominio.modelo.MarcaFrescura;
import com.redsaludandina.legado.dominio.modelo.OrigenHistorial;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Read model del historial canónico con TTL explícito, en legado.historial_read_model (en AWS:
 * ElastiCache/DynamoDB). Guarda el contenido ya traducido y su frescura; lo que se lee de aquí se
 * marca con origen {@link OrigenHistorial#READ_MODEL}.
 */
public final class ReadModelHistorial {

    /** Contenido canónico guardado como JSON. */
    record Contenido(List<EpisodioHistorico> episodios, List<DocumentoDisponible> documentos) {
    }

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public ReadModelHistorial(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public Optional<HistorialClinico> buscar(String pacienteId) {
        return jdbc.sql("SELECT contenido, obtenido_en, expira_en FROM legado.historial_read_model WHERE paciente_id = :id")
                .param("id", pacienteId)
                .query((fila, n) -> {
                    Contenido contenido = json.readValue(fila.getString("contenido"), Contenido.class);
                    return new HistorialClinico(pacienteId, contenido.episodios(), contenido.documentos(),
                            new MarcaFrescura(Jdbc.instante(fila, "obtenido_en"), Jdbc.instante(fila, "expira_en"),
                                    OrigenHistorial.READ_MODEL));
                })
                .optional();
    }

    public void guardar(HistorialClinico historial) {
        String contenido = json.writeValueAsString(new Contenido(historial.episodios(), historial.documentos()));
        MarcaFrescura frescura = historial.frescura();
        int actualizados = jdbc.sql("UPDATE legado.historial_read_model SET contenido = :contenido, obtenido_en = :obtenido,"
                        + " expira_en = :expira, origen = :origen WHERE paciente_id = :id")
                .param("contenido", contenido)
                .param("obtenido", Jdbc.aFecha(frescura.obtenidoEn()))
                .param("expira", Jdbc.aFecha(frescura.expiraEn()))
                .param("origen", frescura.origen().name())
                .param("id", historial.pacienteId())
                .update();
        if (actualizados == 0) {
            jdbc.sql("INSERT INTO legado.historial_read_model (paciente_id, contenido, obtenido_en, expira_en, origen)"
                            + " VALUES (:id, :contenido, :obtenido, :expira, :origen)")
                    .param("id", historial.pacienteId())
                    .param("contenido", contenido)
                    .param("obtenido", Jdbc.aFecha(frescura.obtenidoEn()))
                    .param("expira", Jdbc.aFecha(frescura.expiraEn()))
                    .param("origen", frescura.origen().name())
                    .update();
        }
    }
}
