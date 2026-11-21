package com.redsaludandina.auditoria.infraestructura.adaptador.salida.persistencia;

import com.redsaludandina.auditoria.aplicacion.puerto.salida.RegistroAccesosPort;
import com.redsaludandina.auditoria.dominio.modelo.EventoAcceso;
import com.redsaludandina.auditoria.dominio.modelo.TipoAcceso;
import com.redsaludandina.compartido.infraestructura.persistencia.Jdbc;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Registro de accesos en auditoria.evento_acceso PARA EL PERFIL SIN AWS. Solo inserta y consulta:
 * no hay código que actualice ni borre. En AWS lo reemplaza S3 Object Lock en modo compliance,
 * que además lo impide a nivel de almacenamiento (ADR-04).
 */
public final class RegistroAccesosJdbc implements RegistroAccesosPort {

    private final JdbcClient jdbc;

    public RegistroAccesosJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean agregarSiNoExiste(EventoAcceso evento) {
        Integer existe = jdbc.sql("SELECT COUNT(*) FROM auditoria.evento_acceso WHERE id = :id")
                .param("id", evento.id()).query(Integer.class).single();
        if (existe > 0) {
            return false;
        }
        jdbc.sql("INSERT INTO auditoria.evento_acceso (id, tipo, usuario_id, recurso, motivo, ocurrido_en, detalle)"
                        + " VALUES (:id, :tipo, :usuario, :recurso, :motivo, :ocurrido, :detalle)")
                .param("id", evento.id())
                .param("tipo", evento.tipo().name())
                .param("usuario", evento.usuarioId())
                .param("recurso", evento.recurso())
                .param("motivo", evento.motivo())
                .param("ocurrido", Jdbc.aFecha(evento.ocurridoEn()))
                .param("detalle", evento.detalle())
                .update();
        return true;
    }

    @Override
    public List<EventoAcceso> porRecurso(String recurso) {
        return jdbc.sql("SELECT id, tipo, usuario_id, recurso, motivo, ocurrido_en, detalle FROM auditoria.evento_acceso"
                        + " WHERE recurso = :recurso")
                .param("recurso", recurso)
                .query((fila, n) -> new EventoAcceso(fila.getObject("id", UUID.class),
                        TipoAcceso.valueOf(fila.getString("tipo")), fila.getString("usuario_id"),
                        fila.getString("recurso"), fila.getString("motivo"), Jdbc.instante(fila, "ocurrido_en"),
                        fila.getString("detalle")))
                .list();
    }
}
