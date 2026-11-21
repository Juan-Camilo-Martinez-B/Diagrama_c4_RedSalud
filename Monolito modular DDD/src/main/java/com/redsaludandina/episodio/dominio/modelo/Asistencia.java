package com.redsaludandina.episodio.dominio.modelo;

import com.redsaludandina.compartido.dominio.Requisitos;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/** Objeto de valor inmutable con lo asistencial de la teleconsulta (motivo, hallazgos, conducta). */
public final class Asistencia {

    private final Map<CampoAsistencia, ValorClinico> valores;

    private Asistencia(Map<CampoAsistencia, ValorClinico> valores) {
        EnumMap<CampoAsistencia, ValorClinico> copia = new EnumMap<>(CampoAsistencia.class);
        copia.putAll(valores);
        this.valores = Collections.unmodifiableMap(copia);
    }

    public static Asistencia vacia() {
        return new Asistencia(Map.of());
    }

    public static Asistencia de(Map<CampoAsistencia, ValorClinico> valores) {
        return new Asistencia(Requisitos.noNulo(valores, "valores"));
    }

    public Optional<ValorClinico> valor(CampoAsistencia campo) {
        return Optional.ofNullable(valores.get(campo));
    }

    public boolean tiene(CampoAsistencia campo) {
        return valores.containsKey(campo);
    }

    /** Devuelve una asistencia nueva con el campo reemplazado; esta no cambia. */
    public Asistencia con(CampoAsistencia campo, ValorClinico valor) {
        EnumMap<CampoAsistencia, ValorClinico> nuevos = new EnumMap<>(CampoAsistencia.class);
        nuevos.putAll(valores);
        nuevos.put(Requisitos.noNulo(campo, "campo"), Requisitos.noNulo(valor, "valor"));
        return new Asistencia(nuevos);
    }

    public Map<CampoAsistencia, ValorClinico> valores() {
        return valores;
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof Asistencia asistencia && valores.equals(asistencia.valores);
    }

    @Override
    public int hashCode() {
        return valores.hashCode();
    }
}
