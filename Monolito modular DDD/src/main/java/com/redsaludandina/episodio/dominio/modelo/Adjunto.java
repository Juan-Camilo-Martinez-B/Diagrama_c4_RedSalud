package com.redsaludandina.episodio.dominio.modelo;

import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.compartido.dominio.Requisitos;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Examen o imagen del episodio. El binario vive cifrado en el almacén de adjuntos (S3 + KMS, sin
 * réplicas fuera de la región); la entidad guarda su clave y su huella SHA-256.
 */
public final class Adjunto {

    private static final Pattern SHA_256 = Pattern.compile("^[0-9a-f]{64}$");

    private final IdClinico id;
    private final TipoAdjunto tipo;
    private final String claveAlmacenamiento;
    private final String sha256;

    public Adjunto(IdClinico id, TipoAdjunto tipo, String claveAlmacenamiento, String sha256) {
        this.id = Requisitos.noNulo(id, "adjuntoId");
        this.tipo = Requisitos.noNulo(tipo, "tipo");
        this.claveAlmacenamiento = Requisitos.texto(claveAlmacenamiento, "claveAlmacenamiento");
        String huella = Requisitos.texto(sha256, "sha256").toLowerCase(Locale.ROOT);
        if (!SHA_256.matcher(huella).matches()) {
            throw new IllegalArgumentException("La huella del adjunto debe ser SHA-256 en hexadecimal");
        }
        this.sha256 = huella;
    }

    /** Mismo id y mismo contenido: es un reenvío del mismo adjunto. */
    public boolean mismoContenidoQue(Adjunto otro) {
        return id.equals(otro.id) && tipo == otro.tipo && sha256.equals(otro.sha256);
    }

    public IdClinico id() {
        return id;
    }

    public TipoAdjunto tipo() {
        return tipo;
    }

    public String claveAlmacenamiento() {
        return claveAlmacenamiento;
    }

    public String sha256() {
        return sha256;
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof Adjunto adjunto && id.equals(adjunto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
