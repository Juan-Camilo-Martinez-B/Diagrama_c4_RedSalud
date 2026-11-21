package com.redsaludandina.insights.application.port;

import com.redsaludandina.insights.application.query.AccessLogLine;
import java.util.List;

/** Lado de lectura de la evidencia de accesos (puerto de salida). */
public interface AccessLogReader {

    /** Lecturas clínicas del recurso, de la más antigua a la más reciente. */
    List<AccessLogLine> clinicalReadsOf(String resource);
}
