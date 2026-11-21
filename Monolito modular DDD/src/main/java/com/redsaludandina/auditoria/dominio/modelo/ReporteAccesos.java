package com.redsaludandina.auditoria.dominio.modelo;

import com.redsaludandina.compartido.dominio.Requisitos;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Responde «quién vio este documento y con qué motivo» (Caso §5). El reporte es reproducible:
 * con los mismos registros devuelve siempre las mismas filas en el mismo orden.
 */
public final class ReporteAccesos {

    private static final Comparator<EventoAcceso> ORDEN_ESTABLE =
            Comparator.comparing(EventoAcceso::ocurridoEn).thenComparing(evento -> evento.id().toString());

    private ReporteAccesos() {
    }

    public static List<EventoAcceso> quienVio(String recurso, Collection<EventoAcceso> registros) {
        String buscado = Requisitos.texto(recurso, "recurso");
        return Requisitos.noNulo(registros, "registros").stream()
                .filter(evento -> evento.tipo() == TipoAcceso.LECTURA_CLINICA)
                .filter(evento -> evento.recurso().equals(buscado))
                .sorted(ORDEN_ESTABLE)
                .toList();
    }
}
