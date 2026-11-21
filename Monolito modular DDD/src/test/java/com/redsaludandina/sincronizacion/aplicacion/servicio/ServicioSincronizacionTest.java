package com.redsaludandina.sincronizacion.aplicacion.servicio;

import static com.redsaludandina.compartido.dominio.IdsDePrueba.id;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.redsaludandina.compartido.aplicacion.AlmacenEnMemoria;
import com.redsaludandina.compartido.aplicacion.Fijos;
import com.redsaludandina.compartido.aplicacion.RegistroDuplicadoException;
import com.redsaludandina.compartido.aplicacion.UnidadDeTrabajoDirecta;
import com.redsaludandina.compartido.aplicacion.mensajeria.OutboxEnMemoria;
import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.compartido.evento.PacienteAtendido;
import com.redsaludandina.compartido.evento.ValorClinicoDescartado;
import com.redsaludandina.episodio.aplicacion.AlmacenAdjuntosDePrueba;
import com.redsaludandina.episodio.aplicacion.puerto.salida.EpisodioRepositorio;
import com.redsaludandina.episodio.aplicacion.servicio.ServicioEpisodio;
import com.redsaludandina.episodio.dominio.modelo.Episodio;
import com.redsaludandina.identidad.aplicacion.puerto.entrada.AutorizarAcceso.ResultadoAutorizacion;
import com.redsaludandina.sincronizacion.aplicacion.ClaveIdempotenciaReutilizadaException;
import com.redsaludandina.sincronizacion.aplicacion.puerto.entrada.SincronizarLote.AperturaEpisodio;
import com.redsaludandina.sincronizacion.aplicacion.puerto.entrada.SincronizarLote.CambioOffline;
import com.redsaludandina.sincronizacion.aplicacion.puerto.entrada.SincronizarLote.CierreTeleconsulta;
import com.redsaludandina.sincronizacion.aplicacion.puerto.entrada.SincronizarLote.Estado;
import com.redsaludandina.sincronizacion.aplicacion.puerto.entrada.SincronizarLote.FirmaReceta;
import com.redsaludandina.sincronizacion.aplicacion.puerto.entrada.SincronizarLote.Item;
import com.redsaludandina.sincronizacion.aplicacion.puerto.entrada.SincronizarLote.Lote;
import com.redsaludandina.sincronizacion.aplicacion.puerto.entrada.SincronizarLote.RegistroAsistencia;
import com.redsaludandina.sincronizacion.aplicacion.puerto.entrada.SincronizarLote.ReservaCupo;
import com.redsaludandina.sincronizacion.aplicacion.puerto.entrada.SincronizarLote.Respuesta;
import com.redsaludandina.sincronizacion.aplicacion.puerto.entrada.SincronizarLote.ResultadoItem;
import com.redsaludandina.sincronizacion.aplicacion.puerto.entrada.SincronizarLote.ResultadoLote;
import com.redsaludandina.sincronizacion.aplicacion.puerto.salida.RegistroIdempotenciaPort;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Recorre el diagrama de secuencia de sincronización con los servicios REALES de Episodio en
 * memoria: intento 1 (201), reintento con la misma clave (200, misma R), reintento en otra
 * instancia, carrera por la restricción única y rechazo de cupos y recetas firmadas.
 */
class ServicioSincronizacionTest {

    private static final String K1 = "K1-lote-centro-a";
    private static final String EPISODIO = id(42).valor();
    private static final Instant SELLO = Fijos.AHORA.minus(Duration.ofHours(2));

    private final AlmacenEnMemoria<IdClinico, Episodio> episodios = new AlmacenEnMemoria<>(Episodio::id);
    private final OutboxEnMemoria outbox = new OutboxEnMemoria();

    /** Tabla Idempotency-Key compartida por todas las «instancias» del servicio. */
    private final Map<String, RegistroIdempotenciaPort.RespuestaGuardada> tablaIdempotencia = new HashMap<>();
    private final RegistroIdempotenciaPort registro = new RegistroIdempotenciaPort() {
        @Override
        public Optional<RespuestaGuardada> buscar(String clave) {
            return Optional.ofNullable(tablaIdempotencia.get(clave));
        }

        @Override
        public void guardar(String clave, String huella, ResultadoLote resultado) {
            tablaIdempotencia.putIfAbsent(clave, new RespuestaGuardada(huella, resultado));
        }
    };

    private final ServicioEpisodio servicioEpisodio = new ServicioEpisodio(new EpisodioRepositorio() {
        @Override
        public Optional<Episodio> buscar(IdClinico episodioId) {
            return episodios.buscar(episodioId);
        }

        @Override
        public void guardar(Episodio episodio) {
            episodios.guardar(episodio);
        }
    }, new AlmacenAdjuntosDePrueba(), solicitud -> new ResultadoAutorizacion(true, "ok"), new UnidadDeTrabajoDirecta(),
            outbox.registrador("episodio"), Fijos.RELOJ);

    private ServicioSincronizacion nuevaInstancia() {
        return new ServicioSincronizacion(registro, servicioEpisodio, servicioEpisodio, servicioEpisodio, servicioEpisodio);
    }

    private static Item item(String cambio, String agregado, long secuencia, CambioOffline contenido) {
        return new Item(cambio, agregado, secuencia, contenido);
    }

    /** Lote L1: una teleconsulta completa hecha sin conexión, que llega desordenada. */
    private static Lote loteL1() {
        return new Lote("centro-a", List.of(
                item("c3", EPISODIO, 3, new CierreTeleconsulta(EPISODIO)),
                item("c1", EPISODIO, 1, new AperturaEpisodio(EPISODIO, "pac-1", null)),
                item("c2", EPISODIO, 2, new RegistroAsistencia(EPISODIO, "MOTIVO", "Fiebre", SELLO))));
    }

    @Test
    void intento1AplicaEnOrdenPorAgregadoYPersisteLaClaveConLaRespuesta() {
        Respuesta respuesta = nuevaInstancia().sincronizar(K1, loteL1());

        assertThat(respuesta.repetida()).isFalse();
        assertThat(respuesta.resultado().items()).extracting(ResultadoItem::cambioId).containsExactly("c3", "c1", "c2");
        assertThat(respuesta.resultado().items()).extracting(ResultadoItem::estado).containsOnly(Estado.APLICADO);
        assertThat(tablaIdempotencia).containsKey(K1);
        assertThat(outbox.eventosDe(PacienteAtendido.class)).hasSize(1);
    }

    @Test
    void intento2ConLaMismaClaveDevuelveLaMismaRespuestaSinVolverAAplicar() {
        ServicioSincronizacion servicio = nuevaInstancia();
        Respuesta primera = servicio.sincronizar(K1, loteL1());
        int mensajesDespuesDelPrimero = outbox.mensajes().size();

        Respuesta segunda = servicio.sincronizar(K1, loteL1());

        assertThat(segunda.repetida()).isTrue();
        assertThat(segunda.resultado()).isEqualTo(primera.resultado());
        assertThat(outbox.mensajes()).hasSize(mensajesDespuesDelPrimero);
        assertThat(episodios.todos()).hasSize(1);
    }

    @Test
    void intento3EnOtraInstanciaTrasUnReinicioLeeLaClavePersistida() {
        Respuesta primera = nuevaInstancia().sincronizar(K1, loteL1());

        Respuesta enOtraInstancia = nuevaInstancia().sincronizar(K1, loteL1());

        assertThat(enOtraInstancia.repetida()).isTrue();
        assertThat(enOtraInstancia.resultado()).isEqualTo(primera.resultado());
    }

    @Test
    void laMismaClaveConOtroLoteSeRechazaSinAplicarNada() {
        nuevaInstancia().sincronizar(K1, loteL1());
        Lote otro = new Lote("centro-a", List.of(item("x1", id(43).valor(), 1,
                new AperturaEpisodio(id(43).valor(), "pac-2", null))));

        assertThatThrownBy(() -> nuevaInstancia().sincronizar(K1, otro))
                .isInstanceOf(ClaveIdempotenciaReutilizadaException.class);
        assertThat(episodios.todos()).hasSize(1);
    }

    @Test
    void enLaCarreraExtremaLaRestriccionUnicaSeManejaComoYaAplicado() {
        ServicioSincronizacion conCarrera = new ServicioSincronizacion(registro,
                comando -> {
                    throw new RegistroDuplicadoException("episodio ya insertado por otro intento", null);
                },
                servicioEpisodio, servicioEpisodio, servicioEpisodio);
        Lote lote = new Lote("centro-a", List.of(item("c1", EPISODIO, 1, new AperturaEpisodio(EPISODIO, "pac-1", null))));

        Respuesta respuesta = conCarrera.sincronizar("K-carrera", lote);

        assertThat(respuesta.resultado().items()).singleElement()
                .satisfies(resultado -> assertThat(resultado.estado()).isEqualTo(Estado.YA_APLICADO));
    }

    @Test
    void cuposYRecetasFirmadasNoSeResuelvenEnElBorde() {
        Lote lote = new Lote("centro-a", List.of(
                item("r1", id(20).valor(), 1, new ReservaCupo(id(20).valor(), id(21).valor(), "pac-1")),
                item("f1", id(30).valor(), 1, new FirmaReceta(id(30).valor()))));

        Respuesta respuesta = nuevaInstancia().sincronizar("K-servidor", lote);

        assertThat(respuesta.resultado().items()).allSatisfy(resultado -> {
            assertThat(resultado.estado()).isEqualTo(Estado.RECHAZADO);
            assertThat(resultado.codigo()).isEqualTo(ServicioSincronizacion.REQUIERE_SERVIDOR);
        });
    }

    @Test
    void unaRespuestaConPendientesNoSeGuardaYElReintentoLaCompleta() {
        String episodio = id(50).valor();
        Lote soloAsistencia = new Lote("centro-a", List.of(
                item("a1", episodio, 2, new RegistroAsistencia(episodio, "MOTIVO", "Dolor", SELLO)),
                item("a2", episodio, 3, new CierreTeleconsulta(episodio))));

        Respuesta primera = nuevaInstancia().sincronizar("K-pendiente", soloAsistencia);
        assertThat(primera.resultado().items()).extracting(ResultadoItem::estado)
                .containsExactly(Estado.PENDIENTE, Estado.PENDIENTE);
        assertThat(tablaIdempotencia).doesNotContainKey("K-pendiente");

        nuevaInstancia().sincronizar("K-apertura", new Lote("centro-a",
                List.of(item("a0", episodio, 1, new AperturaEpisodio(episodio, "pac-1", null)))));
        Respuesta reintento = nuevaInstancia().sincronizar("K-pendiente", soloAsistencia);

        assertThat(reintento.repetida()).isFalse();
        assertThat(reintento.resultado().items()).extracting(ResultadoItem::estado).containsOnly(Estado.APLICADO);
        assertThat(tablaIdempotencia).containsKey("K-pendiente");
    }

    @Test
    void unConflictoClinicoSeResuelvePorLastWriterWinsYQuedaParaAuditoria() {
        nuevaInstancia().sincronizar(K1, loteL1());
        Lote otroCentro = new Lote("centro-b", List.of(item("b1", EPISODIO, 1,
                new RegistroAsistencia(EPISODIO, "MOTIVO", "Fiebre y tos", SELLO.plusSeconds(60)))));

        nuevaInstancia().sincronizar("K2-centro-b", otroCentro);

        assertThat(outbox.eventosDe(ValorClinicoDescartado.class)).singleElement()
                .satisfies(evento -> {
                    assertThat(evento.valorConservado()).isEqualTo("Fiebre y tos");
                    assertThat(evento.valorDescartado()).isEqualTo("Fiebre");
                });
    }

    @Test
    void sinIdempotencyKeyNoSeAceptaElLote() {
        assertThatThrownBy(() -> nuevaInstancia().sincronizar(" ", loteL1()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
