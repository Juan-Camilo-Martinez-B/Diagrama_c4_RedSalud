package com.redsaludandina.auditoria.aplicacion.puerto.entrada;

import com.redsaludandina.auditoria.dominio.modelo.EventoAcceso;
import java.util.List;

/** Reporte reproducible: quién vio este recurso y con qué motivo (Caso §5). */
public interface ConsultarQuienVio {

    List<EventoAcceso> quienVio(String recurso);
}
