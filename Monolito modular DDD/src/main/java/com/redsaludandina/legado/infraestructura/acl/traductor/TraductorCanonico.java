package com.redsaludandina.legado.infraestructura.acl.traductor;

import com.redsaludandina.legado.dominio.modelo.DocumentoDisponible;
import com.redsaludandina.legado.dominio.modelo.EpisodioHistorico;
import com.redsaludandina.legado.dominio.modelo.HistorialClinico;
import com.redsaludandina.legado.dominio.modelo.MarcaFrescura;
import com.redsaludandina.legado.infraestructura.acl.modelo.HistorialLegado;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * ÚNICO lugar donde se traduce el modelo del legado al modelo canónico del dominio (Caso §7): si
 * el legado cambia de proveedor, cambia esta clase y nada más.
 *
 * <ul>
 *   <li>Fechas {@code yyyyMMdd} → {@link LocalDate}.</li>
 *   <li>Códigos de tipo de documento → nombres del lenguaje ubicuo.</li>
 *   <li>Glosas vacías → «(sin glosa)», para no propagar nulos del legado.</li>
 * </ul>
 */
public final class TraductorCanonico {

    private static final Map<String, String> TIPOS_DOCUMENTO = Map.of(
            "LAB", "Laboratorio",
            "RX", "Radiografía",
            "ECO", "Ecografía",
            "INF", "Informe");

    /** SUPUESTO: el legado identifica al paciente con el mismo código que el dominio. */
    public String codigoLegado(String pacienteId) {
        return pacienteId;
    }

    public HistorialClinico traducir(HistorialLegado legado, MarcaFrescura frescura) {
        return new HistorialClinico(
                legado.codPaciente().strip(),
                legado.atenciones().stream()
                        .map(atencion -> new EpisodioHistorico(atencion.nroAtencion().strip(), fecha(atencion.fecAtencion()),
                                atencion.glosa() == null || atencion.glosa().isBlank() ? "(sin glosa)" : atencion.glosa().strip()))
                        .toList(),
                legado.documentos().stream()
                        .map(documento -> new DocumentoDisponible(documento.codDocumento().strip(),
                                tipoDocumento(documento.tipDocumento()), fecha(documento.fecDocumento())))
                        .toList(),
                frescura);
    }

    private static String tipoDocumento(String codigo) {
        String limpio = codigo == null ? "" : codigo.strip().toUpperCase();
        return TIPOS_DOCUMENTO.getOrDefault(limpio, "Otro (" + limpio + ")");
    }

    private static LocalDate fecha(String yyyymmdd) {
        try {
            return LocalDate.parse(yyyymmdd.strip(), DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeParseException | NullPointerException error) {
            throw new IllegalStateException("Fecha inválida en el legado: " + yyyymmdd, error);
        }
    }
}
