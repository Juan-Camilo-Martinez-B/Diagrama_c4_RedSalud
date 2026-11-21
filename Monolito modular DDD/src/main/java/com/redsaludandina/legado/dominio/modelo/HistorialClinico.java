package com.redsaludandina.legado.dominio.modelo;

import com.redsaludandina.compartido.dominio.Requisitos;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Read model CANÓNICO del historial clínico (contexto Integración legado, CQRS + ACL, ADR-02).
 *
 * <p>Regla dura del caso (§3): el legado es la fuente de verdad del historial; esta es una copia
 * de lectura con fecha de caducidad visible. Por eso no tiene operaciones que lo modifiquen: un
 * historial nuevo se obtiene de nuevo del legado y reemplaza a este.
 */
public final class HistorialClinico {

    private final String pacienteId;
    private final List<EpisodioHistorico> episodios;
    private final List<DocumentoDisponible> documentos;
    private final MarcaFrescura frescura;

    public HistorialClinico(String pacienteId, List<EpisodioHistorico> episodios,
                            List<DocumentoDisponible> documentos, MarcaFrescura frescura) {
        this.pacienteId = Requisitos.texto(pacienteId, "pacienteId");
        this.episodios = Requisitos.noNulo(episodios, "episodios").stream()
                .sorted(Comparator.comparing(EpisodioHistorico::fecha).reversed())
                .toList();
        this.documentos = Requisitos.noNulo(documentos, "documentos").stream()
                .sorted(Comparator.comparing(DocumentoDisponible::fecha).reversed())
                .toList();
        this.frescura = Requisitos.noNulo(frescura, "frescura");
    }

    /** La copia sigue valiendo; si no, hay que volver a pedirla al legado. */
    public boolean estaVigente(Instant ahora) {
        return frescura.estaVigente(ahora);
    }

    public Instant caducaEn() {
        return frescura.expiraEn();
    }

    public String pacienteId() {
        return pacienteId;
    }

    /** Episodios del más reciente al más antiguo. */
    public List<EpisodioHistorico> episodios() {
        return episodios;
    }

    /** Documentos del más reciente al más antiguo. */
    public List<DocumentoDisponible> documentos() {
        return documentos;
    }

    public MarcaFrescura frescura() {
        return frescura;
    }
}
