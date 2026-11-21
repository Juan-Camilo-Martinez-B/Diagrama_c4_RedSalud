package com.redsaludandina.sincronizacion.infraestructura.adaptador.entrada.web;

import com.redsaludandina.sincronizacion.aplicacion.ClaveIdempotenciaReutilizadaException;
import com.redsaludandina.sincronizacion.aplicacion.puerto.entrada.SincronizarLote;
import com.redsaludandina.sincronizacion.aplicacion.puerto.entrada.SincronizarLote.AdjuntoArchivo;
import com.redsaludandina.sincronizacion.aplicacion.puerto.entrada.SincronizarLote.AperturaEpisodio;
import com.redsaludandina.sincronizacion.aplicacion.puerto.entrada.SincronizarLote.CambioOffline;
import com.redsaludandina.sincronizacion.aplicacion.puerto.entrada.SincronizarLote.CierreTeleconsulta;
import com.redsaludandina.sincronizacion.aplicacion.puerto.entrada.SincronizarLote.FirmaReceta;
import com.redsaludandina.sincronizacion.aplicacion.puerto.entrada.SincronizarLote.Item;
import com.redsaludandina.sincronizacion.aplicacion.puerto.entrada.SincronizarLote.Lote;
import com.redsaludandina.sincronizacion.aplicacion.puerto.entrada.SincronizarLote.RegistroAsistencia;
import com.redsaludandina.sincronizacion.aplicacion.puerto.entrada.SincronizarLote.ReservaCupo;
import com.redsaludandina.sincronizacion.aplicacion.puerto.entrada.SincronizarLote.Respuesta;
import com.redsaludandina.sincronizacion.aplicacion.puerto.entrada.SincronizarLote.ResultadoLote;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST /sync/episodios: recibe un lote de la cola offline de un centro con su
 * {@code Idempotency-Key}.
 *
 * <ul>
 *   <li>201 + R: primer intento con esa clave.</li>
 *   <li>200 + la MISMA R: reintento con la misma clave y el mismo lote (sin nuevos inserts).</li>
 *   <li>422: la clave ya se usó con otro lote.</li>
 * </ul>
 *
 * <p>Cada ítem lleva su {@code tipo}: APERTURA_EPISODIO, REGISTRO_ASISTENCIA, ADJUNTO_ARCHIVO,
 * CIERRE_TELECONSULTA, RESERVA_CUPO o FIRMA_RECETA (estos dos últimos se rechazan siempre: no se
 * resuelven en el borde).
 */
@RestController
public class SincronizacionControlador {

    public static final String ENCABEZADO_IDEMPOTENCIA = "Idempotency-Key";

    private final SincronizarLote sincronizarLote;

    public SincronizacionControlador(SincronizarLote sincronizarLote) {
        this.sincronizarLote = sincronizarLote;
    }

    record SolicitudLote(String centroId, List<SolicitudItem> items) {
    }

    record SolicitudItem(String cambioId, String agregadoId, long secuencia, String tipo, String episodioId,
                         String pacienteId, String cupoId, String campo, String texto, Instant selloInstante,
                         String adjuntoId, String tipoAdjunto, String claveAlmacenamiento, String sha256,
                         String reservaId, String recetaId) {
    }

    @PostMapping("/sync/episodios")
    ResponseEntity<ResultadoLote> sincronizar(@RequestHeader(ENCABEZADO_IDEMPOTENCIA) String idempotencyKey,
                                              @RequestBody SolicitudLote solicitud) {
        List<Item> items = solicitud.items() == null ? List.of() : solicitud.items().stream()
                .map(item -> new Item(item.cambioId(), item.agregadoId(), item.secuencia(), cambio(item)))
                .toList();
        Respuesta respuesta = sincronizarLote.sincronizar(idempotencyKey, new Lote(solicitud.centroId(), items));
        return ResponseEntity.status(respuesta.repetida() ? HttpStatus.OK : HttpStatus.CREATED)
                .body(respuesta.resultado());
    }

    @ExceptionHandler(ClaveIdempotenciaReutilizadaException.class)
    ProblemDetail claveReutilizada(ClaveIdempotenciaReutilizadaException error) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, error.getMessage());
        problema.setProperty("codigo", ClaveIdempotenciaReutilizadaException.CODIGO);
        return problema;
    }

    private static CambioOffline cambio(SolicitudItem item) {
        String tipo = item.tipo() == null ? "" : item.tipo().toUpperCase(Locale.ROOT);
        switch (tipo) {
            case "APERTURA_EPISODIO":
                return new AperturaEpisodio(item.episodioId(), item.pacienteId(), item.cupoId());
            case "REGISTRO_ASISTENCIA":
                return new RegistroAsistencia(item.episodioId(), item.campo(), item.texto(), item.selloInstante());
            case "ADJUNTO_ARCHIVO":
                return new AdjuntoArchivo(item.episodioId(), item.adjuntoId(), item.tipoAdjunto(),
                        item.claveAlmacenamiento(), item.sha256());
            case "CIERRE_TELECONSULTA":
                return new CierreTeleconsulta(item.episodioId());
            case "RESERVA_CUPO":
                return new ReservaCupo(item.cupoId(), item.reservaId(), item.pacienteId());
            case "FIRMA_RECETA":
                return new FirmaReceta(item.recetaId());
            default:
                throw new IllegalArgumentException("Tipo de cambio offline desconocido: " + item.tipo());
        }
    }
}
