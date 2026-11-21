package com.redsaludandina.sincronizacion.aplicacion.servicio;

import com.redsaludandina.compartido.aplicacion.RecursoNoEncontradoException;
import com.redsaludandina.compartido.aplicacion.RegistroDuplicadoException;
import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.compartido.dominio.ReglaDeNegocioException;
import com.redsaludandina.compartido.dominio.Requisitos;
import com.redsaludandina.episodio.aplicacion.puerto.entrada.AbrirEpisodio;
import com.redsaludandina.episodio.aplicacion.puerto.entrada.AdjuntarArchivo;
import com.redsaludandina.episodio.aplicacion.puerto.entrada.CerrarTeleconsulta;
import com.redsaludandina.episodio.aplicacion.puerto.entrada.RegistrarAsistencia;
import com.redsaludandina.sincronizacion.aplicacion.ClaveIdempotenciaReutilizadaException;
import com.redsaludandina.sincronizacion.aplicacion.puerto.entrada.SincronizarLote;
import com.redsaludandina.sincronizacion.aplicacion.puerto.salida.RegistroIdempotenciaPort;
import com.redsaludandina.sincronizacion.aplicacion.puerto.salida.RegistroIdempotenciaPort.RespuestaGuardada;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * OHS de sincronización offline, según el diagrama de secuencia de sincronización:
 *
 * <ol>
 *   <li>¿Existe la Idempotency-Key? Si existe con el mismo lote, devuelve la respuesta guardada R
 *       sin aplicar nada; si existe con otro lote, lo rechaza.</li>
 *   <li>Si no existe, aplica cada cambio en el contexto dueño, en orden por agregado y secuencia.
 *       Cada cambio es una transacción con su outbox; la restricción única del ULID hace que un
 *       duplicado concurrente sea un error manejable (YA_APLICADO).</li>
 *   <li>Persiste la clave con R solo si R es definitiva (sin pendientes).</li>
 * </ol>
 *
 * <p>Cupos y recetas firmadas no se resuelven en el borde: se rechazan con REQUIERE_SERVIDOR.
 * Si el proceso cae entre aplicar los cambios y guardar la clave, el reintento los vuelve a
 * aplicar y el dominio los reconoce por su ULID: no se duplica nada.
 */
public final class ServicioSincronizacion implements SincronizarLote {

    public static final String DATOS_INVALIDOS = "DATOS_INVALIDOS";
    public static final String NO_DISPONIBLE = "NO_DISPONIBLE";
    public static final String ORDEN_PENDIENTE = "ORDEN_PENDIENTE";
    public static final String REQUIERE_SERVIDOR = "REQUIERE_SERVIDOR";

    private static final int LARGO_MAXIMO_CLAVE = 100;

    private final RegistroIdempotenciaPort idempotencia;
    private final AbrirEpisodio abrirEpisodio;
    private final RegistrarAsistencia registrarAsistencia;
    private final AdjuntarArchivo adjuntarArchivo;
    private final CerrarTeleconsulta cerrarTeleconsulta;

    public ServicioSincronizacion(RegistroIdempotenciaPort idempotencia, AbrirEpisodio abrirEpisodio,
                                  RegistrarAsistencia registrarAsistencia, AdjuntarArchivo adjuntarArchivo,
                                  CerrarTeleconsulta cerrarTeleconsulta) {
        this.idempotencia = Requisitos.noNulo(idempotencia, "idempotencia");
        this.abrirEpisodio = Requisitos.noNulo(abrirEpisodio, "abrirEpisodio");
        this.registrarAsistencia = Requisitos.noNulo(registrarAsistencia, "registrarAsistencia");
        this.adjuntarArchivo = Requisitos.noNulo(adjuntarArchivo, "adjuntarArchivo");
        this.cerrarTeleconsulta = Requisitos.noNulo(cerrarTeleconsulta, "cerrarTeleconsulta");
    }

    @Override
    public Respuesta sincronizar(String idempotencyKey, Lote lote) {
        String clave = Requisitos.texto(idempotencyKey, "Idempotency-Key");
        if (clave.length() > LARGO_MAXIMO_CLAVE) {
            throw new IllegalArgumentException("La Idempotency-Key no puede superar " + LARGO_MAXIMO_CLAVE + " caracteres");
        }
        Requisitos.noNulo(lote, "lote");
        String huella = huella(lote);

        Optional<RespuestaGuardada> guardada = idempotencia.buscar(clave);
        if (guardada.isPresent()) {
            if (!guardada.get().huella().equals(huella)) {
                throw new ClaveIdempotenciaReutilizadaException(clave);
            }
            return new Respuesta(guardada.get().resultado(), true);
        }

        ResultadoLote resultado = aplicar(lote);
        if (resultado.definitivo()) {
            idempotencia.guardar(clave, huella, resultado);
        }
        return new Respuesta(resultado, false);
    }

    private ResultadoLote aplicar(Lote lote) {
        String centroId = Requisitos.texto(lote.centroId(), "centroId");
        List<Item> items = Requisitos.noNulo(lote.items(), "items");
        List<Integer> enOrden = IntStream.range(0, items.size()).boxed()
                .sorted(Comparator.comparing((Integer i) -> String.valueOf(items.get(i).agregadoId()))
                        .thenComparingLong(i -> items.get(i).secuencia()))
                .toList();

        ResultadoItem[] resultados = new ResultadoItem[items.size()];
        Set<String> agregadosDetenidos = new HashSet<>();
        for (int indice : enOrden) {
            Item item = items.get(indice);
            ResultadoItem resultado = agregadosDetenidos.contains(item.agregadoId())
                    ? new ResultadoItem(item.cambioId(), Estado.PENDIENTE, ORDEN_PENDIENTE,
                            "Espera a que se aplique un cambio anterior del mismo agregado")
                    : procesar(centroId, item);
            if (resultado.estado() == Estado.PENDIENTE) {
                agregadosDetenidos.add(item.agregadoId());
            }
            resultados[indice] = resultado;
        }
        return new ResultadoLote(List.of(resultados));
    }

    private ResultadoItem procesar(String centroId, Item item) {
        String cambioId = item.cambioId();
        try {
            aplicar(centroId, Requisitos.noNulo(item.cambio(), "cambio"));
            return new ResultadoItem(cambioId, Estado.APLICADO, null, null);
        } catch (RegistroDuplicadoException carrera) {
            return new ResultadoItem(cambioId, Estado.YA_APLICADO, null, "Otro intento ya lo había aplicado");
        } catch (ReglaDeNegocioException rechazo) {
            return new ResultadoItem(cambioId, Estado.RECHAZADO, rechazo.codigo(), rechazo.getMessage());
        } catch (RecursoNoEncontradoException faltaAnterior) {
            return new ResultadoItem(cambioId, Estado.PENDIENTE, NO_DISPONIBLE, faltaAnterior.getMessage());
        } catch (IllegalArgumentException datosInvalidos) {
            return new ResultadoItem(cambioId, Estado.RECHAZADO, DATOS_INVALIDOS, datosInvalidos.getMessage());
        } catch (RuntimeException transitorio) {
            return new ResultadoItem(cambioId, Estado.PENDIENTE, NO_DISPONIBLE, "No se pudo aplicar; reintente");
        }
    }

    private void aplicar(String centroId, CambioOffline cambio) {
        if (cambio instanceof SincronizarLote.AperturaEpisodio abrir) {
            abrirEpisodio.abrir(new AbrirEpisodio.Comando(IdClinico.de(abrir.episodioId()), abrir.pacienteId(),
                    abrir.cupoId() == null ? null : IdClinico.de(abrir.cupoId()), centroId));
        } else if (cambio instanceof SincronizarLote.RegistroAsistencia asistencia) {
            registrarAsistencia.registrar(new RegistrarAsistencia.Comando(IdClinico.de(asistencia.episodioId()),
                    asistencia.campo(), asistencia.texto(), centroId, asistencia.selloInstante()));
        } else if (cambio instanceof SincronizarLote.AdjuntoArchivo adjunto) {
            adjuntarArchivo.adjuntar(new AdjuntarArchivo.Comando(IdClinico.de(adjunto.episodioId()),
                    IdClinico.de(adjunto.adjuntoId()), adjunto.tipo(), adjunto.claveAlmacenamiento(), adjunto.sha256()));
        } else if (cambio instanceof SincronizarLote.CierreTeleconsulta cierre) {
            cerrarTeleconsulta.cerrar(IdClinico.de(cierre.episodioId()));
        } else if (cambio instanceof SincronizarLote.ReservaCupo || cambio instanceof SincronizarLote.FirmaReceta) {
            throw new ReglaDeNegocioException(REQUIERE_SERVIDOR,
                    "Cupos y recetas firmadas no se resuelven en el borde: reintente contra el servidor");
        } else {
            throw new IllegalArgumentException("Cambio offline no soportado: " + cambio.getClass().getSimpleName());
        }
    }

    /** SHA-256 del lote: identifica el contenido para no aceptar otra petición con la misma clave. */
    static String huella(Lote lote) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256").digest(lote.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException imposible) {
            throw new IllegalStateException("SHA-256 no disponible", imposible);
        }
    }
}
