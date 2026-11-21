package com.redsaludandina.interconsulta.infraestructura.adaptador.salida.persistencia;

import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.compartido.infraestructura.persistencia.Jdbc;
import com.redsaludandina.interconsulta.aplicacion.puerto.salida.InterconsultaRepositorio;
import com.redsaludandina.interconsulta.dominio.modelo.Alerta;
import com.redsaludandina.interconsulta.dominio.modelo.EstadoInterconsulta;
import com.redsaludandina.interconsulta.dominio.modelo.Interconsulta;
import com.redsaludandina.interconsulta.dominio.modelo.TipoAlerta;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Interconsultas en el esquema interconsulta, con sus alertas en orden de creación. */
public final class InterconsultaRepositorioJdbc implements InterconsultaRepositorio {

    private final JdbcClient jdbc;

    public InterconsultaRepositorioJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Interconsulta> buscar(IdClinico interconsultaId) {
        List<Alerta> alertas = jdbc.sql("SELECT tipo, creada_en, resuelta_en FROM interconsulta.alerta"
                        + " WHERE interconsulta_id = :id ORDER BY posicion")
                .param("id", interconsultaId.valor())
                .query((fila, n) -> new Alerta(TipoAlerta.valueOf(fila.getString("tipo")),
                        Jdbc.instante(fila, "creada_en"), Jdbc.instante(fila, "resuelta_en")))
                .list();
        return jdbc.sql("SELECT episodio_origen_id, paciente_id, centro_id, especialidad, motivo, estado, cupo_id,"
                        + " episodio_atencion_id FROM interconsulta.interconsulta WHERE id = :id FOR UPDATE")
                .param("id", interconsultaId.valor())
                .query((fila, n) -> Interconsulta.reconstituir(interconsultaId,
                        IdClinico.de(fila.getString("episodio_origen_id")), fila.getString("paciente_id"),
                        fila.getString("centro_id"), fila.getString("especialidad"), fila.getString("motivo"),
                        EstadoInterconsulta.valueOf(fila.getString("estado")), idONulo(fila.getString("cupo_id")),
                        idONulo(fila.getString("episodio_atencion_id")), alertas))
                .optional();
    }

    @Override
    public void guardar(Interconsulta interconsulta) {
        String id = interconsulta.id().valor();
        int actualizados = jdbc.sql("UPDATE interconsulta.interconsulta SET estado = :estado, cupo_id = :cupo,"
                        + " episodio_atencion_id = :atencion WHERE id = :id")
                .param("estado", interconsulta.estado().name())
                .param("cupo", interconsulta.cupoId().map(IdClinico::valor).orElse(null))
                .param("atencion", interconsulta.episodioAtencionId().map(IdClinico::valor).orElse(null))
                .param("id", id)
                .update();
        if (actualizados == 0) {
            jdbc.sql("INSERT INTO interconsulta.interconsulta (id, episodio_origen_id, paciente_id, centro_id,"
                            + " especialidad, motivo, estado, cupo_id, episodio_atencion_id) VALUES (:id, :origen,"
                            + " :paciente, :centro, :especialidad, :motivo, :estado, :cupo, :atencion)")
                    .param("id", id)
                    .param("origen", interconsulta.episodioOrigenId().valor())
                    .param("paciente", interconsulta.pacienteId())
                    .param("centro", interconsulta.centroId())
                    .param("especialidad", interconsulta.especialidad())
                    .param("motivo", interconsulta.motivo())
                    .param("estado", interconsulta.estado().name())
                    .param("cupo", interconsulta.cupoId().map(IdClinico::valor).orElse(null))
                    .param("atencion", interconsulta.episodioAtencionId().map(IdClinico::valor).orElse(null))
                    .update();
        }
        jdbc.sql("DELETE FROM interconsulta.alerta WHERE interconsulta_id = :id").param("id", id).update();
        List<Alerta> alertas = interconsulta.alertas();
        for (int posicion = 0; posicion < alertas.size(); posicion++) {
            Alerta alerta = alertas.get(posicion);
            jdbc.sql("INSERT INTO interconsulta.alerta (interconsulta_id, posicion, tipo, creada_en, resuelta_en)"
                            + " VALUES (:id, :posicion, :tipo, :creada, :resuelta)")
                    .param("id", id)
                    .param("posicion", posicion)
                    .param("tipo", alerta.tipo().name())
                    .param("creada", Jdbc.aFecha(alerta.creadaEn()))
                    .param("resuelta", Jdbc.aFecha(alerta.resueltaEn().orElse(null)))
                    .update();
        }
    }

    @Override
    public Optional<Interconsulta> buscarPorCupo(IdClinico cupoId) {
        return jdbc.sql("SELECT id FROM interconsulta.interconsulta WHERE cupo_id = :cupo")
                .param("cupo", cupoId.valor())
                .query(String.class)
                .optional()
                .flatMap(id -> buscar(IdClinico.de(id)));
    }

    @Override
    public List<Interconsulta> solicitadasDelPaciente(String pacienteId) {
        return cargar(jdbc.sql("SELECT i.id FROM interconsulta.interconsulta i JOIN interconsulta.alerta a"
                        + " ON a.interconsulta_id = i.id AND a.posicion = 0"
                        + " WHERE i.paciente_id = :paciente AND i.estado = 'SOLICITADA' ORDER BY a.creada_en, i.id")
                .param("paciente", pacienteId)
                .query(String.class)
                .list());
    }

    @Override
    public List<Interconsulta> conAlertasPendientes(String centroId) {
        return cargar(jdbc.sql("SELECT DISTINCT i.id FROM interconsulta.interconsulta i JOIN interconsulta.alerta a"
                        + " ON a.interconsulta_id = i.id WHERE i.centro_id = :centro AND a.resuelta_en IS NULL ORDER BY i.id")
                .param("centro", centroId)
                .query(String.class)
                .list());
    }

    private List<Interconsulta> cargar(List<String> ids) {
        return ids.stream().map(id -> buscar(IdClinico.de(id)).orElseThrow()).toList();
    }

    private static IdClinico idONulo(String valor) {
        return valor == null ? null : IdClinico.de(valor);
    }
}
