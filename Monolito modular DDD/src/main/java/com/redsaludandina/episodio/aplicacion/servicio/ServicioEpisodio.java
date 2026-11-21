package com.redsaludandina.episodio.aplicacion.servicio;

import com.redsaludandina.compartido.aplicacion.RecursoNoEncontradoException;
import com.redsaludandina.compartido.aplicacion.UnidadDeTrabajo;
import com.redsaludandina.compartido.aplicacion.mensajeria.RegistradorOutbox;
import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.compartido.dominio.Requisitos;
import com.redsaludandina.compartido.dominio.SelloCentro;
import com.redsaludandina.episodio.aplicacion.puerto.entrada.AbrirEpisodio;
import com.redsaludandina.episodio.aplicacion.puerto.entrada.AdjuntarArchivo;
import com.redsaludandina.episodio.aplicacion.puerto.entrada.CerrarTeleconsulta;
import com.redsaludandina.episodio.aplicacion.puerto.entrada.ConsultarEpisodio;
import com.redsaludandina.episodio.aplicacion.puerto.entrada.DescargarAdjunto;
import com.redsaludandina.episodio.aplicacion.puerto.entrada.PrepararSubidaAdjunto;
import com.redsaludandina.episodio.aplicacion.puerto.entrada.RegistrarAsistencia;
import com.redsaludandina.episodio.aplicacion.puerto.salida.AlmacenAdjuntosPort;
import com.redsaludandina.episodio.aplicacion.puerto.salida.AlmacenAdjuntosPort.DescargaPrefirmada;
import com.redsaludandina.episodio.aplicacion.puerto.salida.AlmacenAdjuntosPort.SubidaPrefirmada;
import com.redsaludandina.episodio.aplicacion.puerto.salida.EpisodioRepositorio;
import com.redsaludandina.episodio.dominio.modelo.Adjunto;
import com.redsaludandina.episodio.dominio.modelo.CampoAsistencia;
import com.redsaludandina.episodio.dominio.modelo.Episodio;
import com.redsaludandina.episodio.dominio.modelo.TipoAdjunto;
import com.redsaludandina.identidad.aplicacion.puerto.entrada.AutorizarAcceso;
import com.redsaludandina.identidad.aplicacion.puerto.entrada.AutorizarAcceso.SolicitudAcceso;
import java.time.Clock;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Casos de uso de Episodio clínico. Cada cambio guarda el episodio y sus eventos
 * (PacienteAtendido, ValorClinicoDescartado, LecturaClinica) en el outbox en una sola transacción.
 *
 * <p>Toda lectura de datos clínicos (el episodio o un adjunto) autoriza por rol y atributo FUERA
 * de la transacción de la lectura, para que una denegación quede registrada por Identidad aunque
 * aquí no se lea nada.
 */
public final class ServicioEpisodio implements AbrirEpisodio, RegistrarAsistencia, AdjuntarArchivo, CerrarTeleconsulta,
        ConsultarEpisodio, PrepararSubidaAdjunto, DescargarAdjunto {

    private final EpisodioRepositorio episodios;
    private final AlmacenAdjuntosPort almacen;
    private final AutorizarAcceso autorizacion;
    private final UnidadDeTrabajo unidadDeTrabajo;
    private final RegistradorOutbox outbox;
    private final Clock reloj;

    public ServicioEpisodio(EpisodioRepositorio episodios, AlmacenAdjuntosPort almacen, AutorizarAcceso autorizacion,
                            UnidadDeTrabajo unidadDeTrabajo, RegistradorOutbox outbox, Clock reloj) {
        this.episodios = Requisitos.noNulo(episodios, "episodios");
        this.almacen = Requisitos.noNulo(almacen, "almacen");
        this.autorizacion = Requisitos.noNulo(autorizacion, "autorizacion");
        this.unidadDeTrabajo = Requisitos.noNulo(unidadDeTrabajo, "unidadDeTrabajo");
        this.outbox = Requisitos.noNulo(outbox, "outbox");
        this.reloj = Requisitos.noNulo(reloj, "reloj");
    }

    @Override
    public void abrir(AbrirEpisodio.Comando comando) {
        Requisitos.noNulo(comando, "comando");
        unidadDeTrabajo.ejecutarSinResultado(() -> {
            if (episodios.buscar(comando.episodioId()).isEmpty()) {
                episodios.guardar(Episodio.abrir(comando.episodioId(), comando.pacienteId(), comando.cupoId(),
                        comando.centroId()));
            }
        });
    }

    @Override
    public void registrar(RegistrarAsistencia.Comando comando) {
        Requisitos.noNulo(comando, "comando");
        CampoAsistencia campo = CampoAsistencia.valueOf(
                Requisitos.texto(comando.campo(), "campo").toUpperCase(Locale.ROOT));
        SelloCentro sello = new SelloCentro(comando.centroId(), comando.selloInstante());
        modificar(comando.episodioId(),
                episodio -> episodio.registrarAsistencia(campo, comando.texto(), sello, reloj.instant()));
    }

    /**
     * Registra un adjunto ya subido. La clave debe ser la que asigna el servidor para ese episodio
     * y adjunto: así nadie puede asociar a un episodio el binario de otro paciente.
     */
    @Override
    public void adjuntar(AdjuntarArchivo.Comando comando) {
        Requisitos.noNulo(comando, "comando");
        String claveEsperada = almacen.claveDe(comando.episodioId(), comando.adjuntoId());
        if (!claveEsperada.equals(comando.claveAlmacenamiento())) {
            throw new IllegalArgumentException("La clave del adjunto debe ser " + claveEsperada);
        }
        Adjunto adjunto = new Adjunto(comando.adjuntoId(),
                TipoAdjunto.valueOf(Requisitos.texto(comando.tipo(), "tipo").toUpperCase(Locale.ROOT)),
                claveEsperada, comando.sha256());
        modificar(comando.episodioId(), episodio -> episodio.adjuntar(adjunto));
    }

    @Override
    public void cerrar(IdClinico episodioId) {
        modificar(episodioId, episodio -> episodio.marcarAtendido(reloj.instant()));
    }

    @Override
    public Episodio consultar(ConsultarEpisodio.Consulta consulta) {
        Requisitos.noNulo(consulta, "consulta");
        autorizar(consulta.episodioId(), consulta.usuarioId());
        return unidadDeTrabajo.ejecutar(() -> {
            Episodio episodio = cargar(consulta.episodioId());
            episodio.registrarLectura(consulta.usuarioId(), consulta.motivo(), reloj.instant());
            outbox.registrar(episodio);
            return episodio;
        });
    }

    @Override
    public Subida preparar(PrepararSubidaAdjunto.Comando comando) {
        Requisitos.noNulo(comando, "comando");
        Requisitos.noNulo(comando.adjuntoId(), "adjuntoId");
        autorizar(comando.episodioId(), comando.usuarioId());
        SubidaPrefirmada subida = almacen.prepararSubida(comando.episodioId(), comando.adjuntoId(),
                Requisitos.texto(comando.sha256(), "sha256"));
        return new Subida(subida.url(), subida.encabezados(), subida.claveAlmacenamiento(), subida.expiraEn());
    }

    @Override
    public Descarga descargar(DescargarAdjunto.Consulta consulta) {
        Requisitos.noNulo(consulta, "consulta");
        autorizar(consulta.episodioId(), consulta.usuarioId());
        String clave = unidadDeTrabajo.ejecutar(() -> {
            Episodio episodio = cargar(consulta.episodioId());
            Adjunto adjunto = episodio.adjuntos().stream()
                    .filter(candidato -> candidato.id().equals(consulta.adjuntoId()))
                    .findFirst()
                    .orElseThrow(() -> new RecursoNoEncontradoException("el adjunto " + consulta.adjuntoId()));
            episodio.registrarLecturaDeAdjunto(adjunto.id(), consulta.usuarioId(), consulta.motivo(), reloj.instant());
            outbox.registrar(episodio);
            return adjunto.claveAlmacenamiento();
        });
        DescargaPrefirmada descarga = almacen.prepararDescarga(clave);
        return new Descarga(descarga.url(), descarga.expiraEn());
    }

    /** Autoriza con los atributos del episodio (paciente y centro), fuera de la transacción. */
    private void autorizar(IdClinico episodioId, String usuarioId) {
        Episodio leido = unidadDeTrabajo.ejecutar(() -> cargar(episodioId));
        autorizacion.exigir(new SolicitudAcceso(usuarioId, "episodio/" + leido.id(), leido.pacienteId(),
                leido.centroId(), null));
    }

    private void modificar(IdClinico episodioId, Consumer<Episodio> cambio) {
        unidadDeTrabajo.ejecutarSinResultado(() -> {
            Episodio episodio = cargar(episodioId);
            cambio.accept(episodio);
            episodios.guardar(episodio);
            outbox.registrar(episodio);
        });
    }

    private Episodio cargar(IdClinico episodioId) {
        Requisitos.noNulo(episodioId, "episodioId");
        return episodios.buscar(episodioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("el episodio " + episodioId));
    }
}
