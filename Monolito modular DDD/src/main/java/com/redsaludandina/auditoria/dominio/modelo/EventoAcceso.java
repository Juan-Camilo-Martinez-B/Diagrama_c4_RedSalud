package com.redsaludandina.auditoria.dominio.modelo;

import com.redsaludandina.compartido.dominio.Requisitos;
import com.redsaludandina.compartido.evento.LecturaClinica;
import com.redsaludandina.compartido.evento.ValorClinicoDescartado;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Raíz del agregado de Auditoría: un registro INMUTABLE de acceso a la historia clínica con
 * usuario, fecha, recurso y motivo (Caso §5). Solo se crea, nunca se modifica ni se borra: se
 * escribe en un almacén append-only (S3 Object Lock, ADR-04). Auditoría es conformista con el
 * lenguaje publicado: guarda los hechos tal como llegan.
 */
public final class EventoAcceso {

    /** Actor con el que se registran los valores que descartó la sincronización. */
    public static final String SISTEMA_SINCRONIZACION = "sistema:sincronizacion";

    private final UUID id;
    private final TipoAcceso tipo;
    private final String usuarioId;
    private final String recurso;
    private final String motivo;
    private final Instant ocurridoEn;
    private final String detalle;

    public EventoAcceso(UUID id, TipoAcceso tipo, String usuarioId, String recurso, String motivo,
                        Instant ocurridoEn, String detalle) {
        this.id = Requisitos.noNulo(id, "id");
        this.tipo = Requisitos.noNulo(tipo, "tipo");
        this.usuarioId = Requisitos.texto(usuarioId, "usuarioId");
        this.recurso = Requisitos.texto(recurso, "recurso");
        this.motivo = Requisitos.texto(motivo, "motivo");
        this.ocurridoEn = Requisitos.noNulo(ocurridoEn, "ocurridoEn");
        this.detalle = detalle == null ? "" : detalle;
    }

    public static EventoAcceso desde(LecturaClinica lectura, UUID id) {
        return new EventoAcceso(id, TipoAcceso.LECTURA_CLINICA, lectura.usuarioId(), lectura.recurso(),
                lectura.motivo(), lectura.ocurridoEn(), "");
    }

    public static EventoAcceso desde(ValorClinicoDescartado descarte, UUID id) {
        String detalle = "campo=" + descarte.campo()
                + "; conservado=\"" + descarte.valorConservado() + "\" (" + descarte.selloConservado() + ")"
                + "; descartado=\"" + descarte.valorDescartado() + "\" (" + descarte.selloDescartado() + ")";
        return new EventoAcceso(id, TipoAcceso.VALOR_CLINICO_DESCARTADO, SISTEMA_SINCRONIZACION,
                "episodio/" + descarte.episodioId(), "Resolución last-writer-wins", descarte.ocurridoEn(), detalle);
    }

    public UUID id() {
        return id;
    }

    public TipoAcceso tipo() {
        return tipo;
    }

    public String usuarioId() {
        return usuarioId;
    }

    public String recurso() {
        return recurso;
    }

    public String motivo() {
        return motivo;
    }

    public Instant ocurridoEn() {
        return ocurridoEn;
    }

    public String detalle() {
        return detalle;
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof EventoAcceso evento && id.equals(evento.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
