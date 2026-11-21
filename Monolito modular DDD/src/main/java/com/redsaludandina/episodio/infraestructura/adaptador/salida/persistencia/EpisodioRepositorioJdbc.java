package com.redsaludandina.episodio.infraestructura.adaptador.salida.persistencia;

import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.compartido.dominio.SelloCentro;
import com.redsaludandina.compartido.infraestructura.persistencia.Jdbc;
import com.redsaludandina.episodio.aplicacion.puerto.salida.EpisodioRepositorio;
import com.redsaludandina.episodio.dominio.modelo.Adjunto;
import com.redsaludandina.episodio.dominio.modelo.Asistencia;
import com.redsaludandina.episodio.dominio.modelo.CampoAsistencia;
import com.redsaludandina.episodio.dominio.modelo.Episodio;
import com.redsaludandina.episodio.dominio.modelo.EstadoEpisodio;
import com.redsaludandina.episodio.dominio.modelo.TipoAdjunto;
import com.redsaludandina.episodio.dominio.modelo.ValorClinico;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Episodios en el esquema episodio. El agregado se guarda completo: la fila raíz, la asistencia
 * (con el sello de cada campo, que necesita last-writer-wins) y la referencia de cada adjunto.
 * {@link #buscar} bloquea la fila raíz para que dos sincronizaciones del mismo episodio no se pisen.
 */
public final class EpisodioRepositorioJdbc implements EpisodioRepositorio {

    private final JdbcClient jdbc;

    public EpisodioRepositorioJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Episodio> buscar(IdClinico episodioId) {
        record Raiz(String pacienteId, String cupoId, String centroId, String estado, java.time.Instant atendidoEn) {
        }
        Optional<Raiz> raiz = jdbc.sql("SELECT paciente_id, cupo_id, centro_id, estado, atendido_en"
                        + " FROM episodio.episodio WHERE id = :id FOR UPDATE")
                .param("id", episodioId.valor())
                .query((fila, n) -> new Raiz(fila.getString("paciente_id"), fila.getString("cupo_id"),
                        fila.getString("centro_id"), fila.getString("estado"), Jdbc.instante(fila, "atendido_en")))
                .optional();
        if (raiz.isEmpty()) {
            return Optional.empty();
        }
        Map<CampoAsistencia, ValorClinico> valores = new EnumMap<>(CampoAsistencia.class);
        jdbc.sql("SELECT campo, texto, sello_centro, sello_instante FROM episodio.asistencia WHERE episodio_id = :id")
                .param("id", episodioId.valor())
                .query((fila, n) -> {
                    valores.put(CampoAsistencia.valueOf(fila.getString("campo")), new ValorClinico(fila.getString("texto"),
                            new SelloCentro(fila.getString("sello_centro"), Jdbc.instante(fila, "sello_instante"))));
                    return null;
                })
                .list();
        List<Adjunto> adjuntos = jdbc.sql("SELECT id, tipo, clave_almacenamiento, sha256 FROM episodio.adjunto"
                        + " WHERE episodio_id = :id ORDER BY id")
                .param("id", episodioId.valor())
                .query((fila, n) -> new Adjunto(IdClinico.de(fila.getString("id")),
                        TipoAdjunto.valueOf(fila.getString("tipo")), fila.getString("clave_almacenamiento"),
                        fila.getString("sha256")))
                .list();
        Raiz r = raiz.get();
        return Optional.of(Episodio.reconstituir(episodioId, r.pacienteId(),
                r.cupoId() == null ? null : IdClinico.de(r.cupoId()), r.centroId(), EstadoEpisodio.valueOf(r.estado()),
                Asistencia.de(valores), adjuntos, r.atendidoEn()));
    }

    @Override
    public void guardar(Episodio episodio) {
        String id = episodio.id().valor();
        int actualizados = jdbc.sql("UPDATE episodio.episodio SET estado = :estado, atendido_en = :atendido WHERE id = :id")
                .param("estado", episodio.estado().name())
                .param("atendido", Jdbc.aFecha(episodio.atendidoEn().orElse(null)))
                .param("id", id)
                .update();
        if (actualizados == 0) {
            jdbc.sql("INSERT INTO episodio.episodio (id, paciente_id, cupo_id, centro_id, estado, atendido_en)"
                            + " VALUES (:id, :paciente, :cupo, :centro, :estado, :atendido)")
                    .param("id", id)
                    .param("paciente", episodio.pacienteId())
                    .param("cupo", episodio.cupoId().map(IdClinico::valor).orElse(null))
                    .param("centro", episodio.centroId())
                    .param("estado", episodio.estado().name())
                    .param("atendido", Jdbc.aFecha(episodio.atendidoEn().orElse(null)))
                    .update();
        }
        jdbc.sql("DELETE FROM episodio.asistencia WHERE episodio_id = :id").param("id", id).update();
        episodio.asistencia().valores().forEach((campo, valor) ->
                jdbc.sql("INSERT INTO episodio.asistencia (episodio_id, campo, texto, sello_centro, sello_instante)"
                                + " VALUES (:id, :campo, :texto, :centro, :instante)")
                        .param("id", id)
                        .param("campo", campo.name())
                        .param("texto", valor.texto())
                        .param("centro", valor.sello().centroId())
                        .param("instante", Jdbc.aFecha(valor.sello().instante()))
                        .update());
        for (Adjunto adjunto : episodio.adjuntos()) {
            Integer existe = jdbc.sql("SELECT COUNT(*) FROM episodio.adjunto WHERE id = :adjunto")
                    .param("adjunto", adjunto.id().valor()).query(Integer.class).single();
            if (existe == 0) {
                jdbc.sql("INSERT INTO episodio.adjunto (id, episodio_id, tipo, clave_almacenamiento, sha256)"
                                + " VALUES (:adjunto, :id, :tipo, :clave, :sha)")
                        .param("adjunto", adjunto.id().valor())
                        .param("id", id)
                        .param("tipo", adjunto.tipo().name())
                        .param("clave", adjunto.claveAlmacenamiento())
                        .param("sha", adjunto.sha256())
                        .update();
            }
        }
    }
}
