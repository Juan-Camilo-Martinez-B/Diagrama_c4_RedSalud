package com.redsaludandina.episodio.infraestructura.adaptador.entrada.web;

import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.compartido.infraestructura.web.UsuarioAutenticado;
import com.redsaludandina.episodio.aplicacion.puerto.entrada.AbrirEpisodio;
import com.redsaludandina.episodio.aplicacion.puerto.entrada.AdjuntarArchivo;
import com.redsaludandina.episodio.aplicacion.puerto.entrada.CerrarTeleconsulta;
import com.redsaludandina.episodio.aplicacion.puerto.entrada.ConsultarEpisodio;
import com.redsaludandina.episodio.aplicacion.puerto.entrada.DescargarAdjunto;
import com.redsaludandina.episodio.aplicacion.puerto.entrada.PrepararSubidaAdjunto;
import com.redsaludandina.episodio.aplicacion.puerto.entrada.RegistrarAsistencia;
import com.redsaludandina.episodio.dominio.modelo.Episodio;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** API de Episodio clínico [HTTPS]. */
@RestController
@RequestMapping("/episodios")
public class EpisodioControlador {

    private final AbrirEpisodio abrirEpisodio;
    private final RegistrarAsistencia registrarAsistencia;
    private final AdjuntarArchivo adjuntarArchivo;
    private final CerrarTeleconsulta cerrarTeleconsulta;
    private final ConsultarEpisodio consultarEpisodio;
    private final PrepararSubidaAdjunto prepararSubidaAdjunto;
    private final DescargarAdjunto descargarAdjunto;

    public EpisodioControlador(AbrirEpisodio abrirEpisodio, RegistrarAsistencia registrarAsistencia,
                               AdjuntarArchivo adjuntarArchivo, CerrarTeleconsulta cerrarTeleconsulta,
                               ConsultarEpisodio consultarEpisodio, PrepararSubidaAdjunto prepararSubidaAdjunto,
                               DescargarAdjunto descargarAdjunto) {
        this.abrirEpisodio = abrirEpisodio;
        this.registrarAsistencia = registrarAsistencia;
        this.adjuntarArchivo = adjuntarArchivo;
        this.cerrarTeleconsulta = cerrarTeleconsulta;
        this.consultarEpisodio = consultarEpisodio;
        this.prepararSubidaAdjunto = prepararSubidaAdjunto;
        this.descargarAdjunto = descargarAdjunto;
    }

    record SolicitudSubida(String adjuntoId, String sha256) {
    }

    /** Paso 1 de adjuntar: URL prefirmada y encabezados que el cliente debe enviar en el PUT a S3. */
    @PostMapping("/{episodioId}/adjuntos/subidas")
    PrepararSubidaAdjunto.Subida prepararSubida(@PathVariable String episodioId, @RequestBody SolicitudSubida solicitud,
                                                @UsuarioAutenticado String usuarioId) {
        return prepararSubidaAdjunto.preparar(new PrepararSubidaAdjunto.Comando(IdClinico.de(episodioId),
                IdClinico.de(solicitud.adjuntoId()), solicitud.sha256(), usuarioId));
    }

    /** Lectura de un adjunto: autoriza, audita con el motivo y devuelve una URL de descarga de corta duración. */
    @GetMapping("/{episodioId}/adjuntos/{adjuntoId}")
    DescargarAdjunto.Descarga descargar(@PathVariable String episodioId, @PathVariable String adjuntoId,
                                        @RequestParam String motivo, @UsuarioAutenticado String usuarioId) {
        return descargarAdjunto.descargar(new DescargarAdjunto.Consulta(IdClinico.de(episodioId), IdClinico.de(adjuntoId),
                usuarioId, motivo));
    }

    record SolicitudApertura(String episodioId, String pacienteId, String cupoId, String centroId) {
    }

    record SolicitudAsistencia(String texto, String centroId, Instant selloInstante) {
    }

    record SolicitudAdjunto(String adjuntoId, String tipo, String claveAlmacenamiento, String sha256) {
    }

    record VistaAdjunto(String id, String tipo, String claveAlmacenamiento) {
    }

    record VistaEpisodio(String id, String pacienteId, String centroId, String estado, Map<String, String> asistencia,
                         List<VistaAdjunto> adjuntos) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void abrir(@RequestBody SolicitudApertura solicitud) {
        abrirEpisodio.abrir(new AbrirEpisodio.Comando(IdClinico.de(solicitud.episodioId()), solicitud.pacienteId(),
                solicitud.cupoId() == null ? null : IdClinico.de(solicitud.cupoId()), solicitud.centroId()));
    }

    @PutMapping("/{episodioId}/asistencia/{campo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void registrar(@PathVariable String episodioId, @PathVariable String campo, @RequestBody SolicitudAsistencia solicitud) {
        registrarAsistencia.registrar(new RegistrarAsistencia.Comando(IdClinico.de(episodioId), campo, solicitud.texto(),
                solicitud.centroId(), solicitud.selloInstante()));
    }

    @PostMapping("/{episodioId}/adjuntos")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void adjuntar(@PathVariable String episodioId, @RequestBody SolicitudAdjunto solicitud) {
        adjuntarArchivo.adjuntar(new AdjuntarArchivo.Comando(IdClinico.de(episodioId), IdClinico.de(solicitud.adjuntoId()),
                solicitud.tipo(), solicitud.claveAlmacenamiento(), solicitud.sha256()));
    }

    @PostMapping("/{episodioId}/cierre")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cerrar(@PathVariable String episodioId) {
        cerrarTeleconsulta.cerrar(IdClinico.de(episodioId));
    }

    @GetMapping("/{episodioId}")
    VistaEpisodio consultar(@PathVariable String episodioId, @RequestParam String motivo,
                            @UsuarioAutenticado String usuarioId) {
        Episodio episodio = consultarEpisodio.consultar(
                new ConsultarEpisodio.Consulta(IdClinico.de(episodioId), usuarioId, motivo));
        Map<String, String> asistencia = new LinkedHashMap<>();
        episodio.asistencia().valores().forEach((campo, valor) -> asistencia.put(campo.name(), valor.texto()));
        return new VistaEpisodio(episodio.id().valor(), episodio.pacienteId(), episodio.centroId(),
                episodio.estado().name(), asistencia,
                episodio.adjuntos().stream()
                        .map(adjunto -> new VistaAdjunto(adjunto.id().valor(), adjunto.tipo().name(),
                                adjunto.claveAlmacenamiento()))
                        .toList());
    }
}
