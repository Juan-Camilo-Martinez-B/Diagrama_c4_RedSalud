package com.redsaludandina.legacy.infrastructure.acl.client.db;

import com.redsaludandina.legacy.infrastructure.acl.model.LegacyHistory;
import com.redsaludandina.legacy.infrastructure.acl.model.LegacyHistory.LegacyAttention;
import com.redsaludandina.legacy.infrastructure.acl.model.LegacyHistory.LegacyDocument;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Cliente de SOLO LECTURA a la base del legado (Caso §3). No es el ORM del monolito: es una
 * integración con un esquema ajeno que no controlamos, por eso lee con SQL plano el esquema del
 * legado y corta a los 6 s que fija el caso para la consulta directa.
 */
public final class LegacyDbClient {

    public static final int TIMEOUT_SECONDS = 6;

    private final DataSource dataSource;
    private final JdbcClient jdbc;

    public LegacyDbClient(DataSource dataSource) {
        this.dataSource = dataSource;
        JdbcTemplate template = new JdbcTemplate(dataSource);
        template.setQueryTimeout(TIMEOUT_SECONDS);
        this.jdbc = JdbcClient.create(template);
    }

    public Optional<LegacyHistory> fetch(String codPaciente) {
        return jdbc.sql("SELECT COD_PACIENTE FROM HC_PACIENTE WHERE COD_PACIENTE = :cod")
                .param("cod", codPaciente)
                .query(String.class)
                .optional()
                .map(patient -> new LegacyHistory(patient, attentions(patient), documents(patient)));
    }

    private List<LegacyAttention> attentions(String codPaciente) {
        return jdbc.sql("SELECT NRO_ATENCION, FEC_ATENCION, GLOSA FROM HC_ATENCION WHERE COD_PACIENTE = :cod")
                .param("cod", codPaciente)
                .query((row, number) -> new LegacyAttention(row.getString("NRO_ATENCION"), row.getString("FEC_ATENCION"),
                        row.getString("GLOSA")))
                .list();
    }

    private List<LegacyDocument> documents(String codPaciente) {
        return jdbc.sql("SELECT COD_DOCUMENTO, TIP_DOCUMENTO, FEC_DOCUMENTO FROM HC_DOCUMENTO WHERE COD_PACIENTE = :cod")
                .param("cod", codPaciente)
                .query((row, number) -> new LegacyDocument(row.getString("COD_DOCUMENTO"),
                        row.getString("TIP_DOCUMENTO"), row.getString("FEC_DOCUMENTO")))
                .list();
    }

    public void close() throws Exception {
        if (dataSource instanceof AutoCloseable closeable) {
            closeable.close();
        }
    }
}
