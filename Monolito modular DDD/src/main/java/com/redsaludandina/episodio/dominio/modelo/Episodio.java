package com.redsaludandina.episodio.dominio.modelo;

import com.redsaludandina.compartido.dominio.AgregadoRaiz;
import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.compartido.dominio.ReglaDeNegocioException;
import com.redsaludandina.compartido.dominio.Requisitos;
import com.redsaludandina.compartido.dominio.SelloCentro;
import com.redsaludandina.compartido.evento.LecturaClinica;
import com.redsaludandina.compartido.evento.PacienteAtendido;
import com.redsaludandina.compartido.evento.ValorClinicoDescartado;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Raíz del agregado Episodio (contexto Episodio clínico, Núcleo): la teleconsulta con su
 * asistencia y sus adjuntos, con consistencia fuerte dentro del agregado (Caso §2).
 *
 * <p>Lo asistencial llega también desde centros sin conexión: cada campo se resuelve por
 * last-writer-wins según el {@link SelloCentro}, y el valor que queda fuera se publica en
 * {@link ValorClinicoDescartado} para que Auditoría guarde ambos (Caso §4).
 */
public final class Episodio extends AgregadoRaiz<IdClinico> {

    public static final String ASISTENCIA_SIN_MOTIVO = "ASISTENCIA_SIN_MOTIVO";
    public static final String ADJUNTO_EN_CONFLICTO = "ADJUNTO_EN_CONFLICTO";
    public static final String ADJUNTO_INEXISTENTE = "ADJUNTO_INEXISTENTE";

    private final IdClinico id;
    private final String pacienteId;
    private final IdClinico cupoId;
    private final String centroId;
    private EstadoEpisodio estado;
    private Asistencia asistencia;
    private final Map<IdClinico, Adjunto> adjuntos;
    private Instant atendidoEn;

    private Episodio(IdClinico id, String pacienteId, IdClinico cupoId, String centroId,
                     EstadoEpisodio estado, Asistencia asistencia, Collection<Adjunto> adjuntos,
                     Instant atendidoEn) {
        this.id = Requisitos.noNulo(id, "episodioId");
        this.pacienteId = Requisitos.texto(pacienteId, "pacienteId");
        this.cupoId = cupoId;
        this.centroId = Requisitos.texto(centroId, "centroId");
        this.estado = Requisitos.noNulo(estado, "estado");
        this.asistencia = Requisitos.noNulo(asistencia, "asistencia");
        this.adjuntos = new LinkedHashMap<>();
        adjuntos.forEach(adjunto -> this.adjuntos.put(adjunto.id(), adjunto));
        this.atendidoEn = atendidoEn;
    }

    /** Abre la teleconsulta. {@code cupoId} es nulo si la atención no viene de la agenda. */
    public static Episodio abrir(IdClinico id, String pacienteId, IdClinico cupoId, String centroId) {
        return new Episodio(id, pacienteId, cupoId, centroId, EstadoEpisodio.ABIERTO,
                Asistencia.vacia(), List.of(), null);
    }

    /** Reconstruye un episodio ya guardado; no emite eventos. */
    public static Episodio reconstituir(IdClinico id, String pacienteId, IdClinico cupoId, String centroId,
                                        EstadoEpisodio estado, Asistencia asistencia,
                                        Collection<Adjunto> adjuntos, Instant atendidoEn) {
        return new Episodio(id, pacienteId, cupoId, centroId, estado, asistencia, adjuntos, atendidoEn);
    }

    /**
     * Registra un campo asistencial con last-writer-wins por campo. Gana el sello más reciente;
     * el valor que pierde no se pierde: sale en {@link ValorClinicoDescartado}. Reenviar el mismo
     * valor no produce nada.
     */
    public void registrarAsistencia(CampoAsistencia campo, String texto, SelloCentro sello, Instant ahora) {
        Requisitos.noNulo(campo, "campo");
        Requisitos.noNulo(ahora, "ahora");
        ValorClinico entrante = new ValorClinico(texto, sello);
        Optional<ValorClinico> actual = asistencia.valor(campo);
        if (actual.isEmpty()) {
            asistencia = asistencia.con(campo, entrante);
            return;
        }
        ValorClinico vigente = actual.get();
        if (vigente.equals(entrante)) {
            return;
        }
        boolean ganaEntrante = entrante.sello().esPosteriorA(vigente.sello());
        ValorClinico conservado = ganaEntrante ? entrante : vigente;
        ValorClinico descartado = ganaEntrante ? vigente : entrante;
        if (ganaEntrante) {
            asistencia = asistencia.con(campo, entrante);
        }
        if (!conservado.texto().equals(descartado.texto())) {
            registrar(new ValorClinicoDescartado(id, campo.name(), conservado.texto(), conservado.sello(),
                    descartado.texto(), descartado.sello(), ahora));
        }
    }

    /**
     * Agrega un examen o imagen. Reenviar el mismo adjunto no tiene efecto; otro contenido con el
     * mismo id es un conflicto. Se aceptan adjuntos aun después de la atención (resultados tardíos).
     */
    public void adjuntar(Adjunto adjunto) {
        Requisitos.noNulo(adjunto, "adjunto");
        Adjunto existente = adjuntos.get(adjunto.id());
        if (existente != null) {
            if (existente.mismoContenidoQue(adjunto)) {
                return;
            }
            throw new ReglaDeNegocioException(ADJUNTO_EN_CONFLICTO,
                    "Ya existe otro adjunto con el id " + adjunto.id());
        }
        adjuntos.put(adjunto.id(), adjunto);
    }

    /** Cierra la teleconsulta y publica PacienteAtendido. Exige el motivo de consulta registrado. */
    public void marcarAtendido(Instant ahora) {
        Requisitos.noNulo(ahora, "ahora");
        if (estado == EstadoEpisodio.ATENDIDO) {
            return;
        }
        if (!asistencia.tiene(CampoAsistencia.MOTIVO)) {
            throw new ReglaDeNegocioException(ASISTENCIA_SIN_MOTIVO,
                    "No se puede cerrar la teleconsulta " + id + " sin motivo de consulta");
        }
        estado = EstadoEpisodio.ATENDIDO;
        atendidoEn = ahora;
        registrar(new PacienteAtendido(id, pacienteId, cupoId, centroId, ahora));
    }

    /** Cada lectura de la historia clínica deja rastro con usuario, recurso y motivo (Caso §5). */
    public void registrarLectura(String usuarioId, String motivo, Instant ahora) {
        registrar(new LecturaClinica(usuarioId, "episodio/" + id, motivo, ahora));
    }

    /** Leer un examen o imagen también es leer historia clínica: deja su propio rastro. */
    public void registrarLecturaDeAdjunto(IdClinico adjuntoId, String usuarioId, String motivo, Instant ahora) {
        Requisitos.noNulo(adjuntoId, "adjuntoId");
        if (!adjuntos.containsKey(adjuntoId)) {
            throw new ReglaDeNegocioException(ADJUNTO_INEXISTENTE,
                    "El episodio " + id + " no tiene el adjunto " + adjuntoId);
        }
        registrar(new LecturaClinica(usuarioId, "episodio/" + id + "/adjunto/" + adjuntoId, motivo, ahora));
    }

    @Override
    public IdClinico id() {
        return id;
    }

    public String pacienteId() {
        return pacienteId;
    }

    public Optional<IdClinico> cupoId() {
        return Optional.ofNullable(cupoId);
    }

    public String centroId() {
        return centroId;
    }

    public EstadoEpisodio estado() {
        return estado;
    }

    public Asistencia asistencia() {
        return asistencia;
    }

    public Collection<Adjunto> adjuntos() {
        return Collections.unmodifiableCollection(adjuntos.values());
    }

    public Optional<Instant> atendidoEn() {
        return Optional.ofNullable(atendidoEn);
    }
}
