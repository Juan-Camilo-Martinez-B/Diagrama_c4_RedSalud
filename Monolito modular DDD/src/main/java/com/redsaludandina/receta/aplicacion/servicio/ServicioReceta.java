package com.redsaludandina.receta.aplicacion.servicio;

import com.redsaludandina.compartido.aplicacion.RecursoNoEncontradoException;
import com.redsaludandina.compartido.aplicacion.UnidadDeTrabajo;
import com.redsaludandina.compartido.aplicacion.mensajeria.EventoEntrante;
import com.redsaludandina.compartido.aplicacion.mensajeria.InboxPort;
import com.redsaludandina.compartido.aplicacion.mensajeria.RegistradorOutbox;
import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.compartido.dominio.Requisitos;
import com.redsaludandina.compartido.evento.RecetaFirmada;
import com.redsaludandina.receta.aplicacion.puerto.entrada.AgregarMedicamento;
import com.redsaludandina.receta.aplicacion.puerto.entrada.EnviarRecetaAFarmacia;
import com.redsaludandina.receta.aplicacion.puerto.entrada.FirmarReceta;
import com.redsaludandina.receta.aplicacion.puerto.entrada.RedactarReceta;
import com.redsaludandina.receta.aplicacion.puerto.salida.FarmaciaPort;
import com.redsaludandina.receta.aplicacion.puerto.salida.RecetaRepositorio;
import com.redsaludandina.receta.dominio.modelo.FirmaDigital;
import com.redsaludandina.receta.dominio.modelo.Medicamento;
import com.redsaludandina.receta.dominio.modelo.Receta;
import java.time.Clock;

/**
 * Casos de uso de Receta. Firmar guarda la receta y su RecetaFirmada en el outbox en una sola
 * transacción; el envío a la farmacia ocurre después, al consumir ese evento, y no se repite si
 * la receta ya figura como enviada.
 *
 * <p>Consumidor idempotente: descarta un RecetaFirmada ya procesado por su id de evento (inbox).
 */
public final class ServicioReceta implements RedactarReceta, AgregarMedicamento, FirmarReceta, EnviarRecetaAFarmacia {

    static final String CONSUMIDOR = "receta.enviar-a-farmacia";

    private final RecetaRepositorio recetas;
    private final FarmaciaPort farmacia;
    private final InboxPort inbox;
    private final UnidadDeTrabajo unidadDeTrabajo;
    private final RegistradorOutbox outbox;
    private final Clock reloj;

    public ServicioReceta(RecetaRepositorio recetas, FarmaciaPort farmacia, InboxPort inbox,
                          UnidadDeTrabajo unidadDeTrabajo, RegistradorOutbox outbox, Clock reloj) {
        this.recetas = Requisitos.noNulo(recetas, "recetas");
        this.farmacia = Requisitos.noNulo(farmacia, "farmacia");
        this.inbox = Requisitos.noNulo(inbox, "inbox");
        this.unidadDeTrabajo = Requisitos.noNulo(unidadDeTrabajo, "unidadDeTrabajo");
        this.outbox = Requisitos.noNulo(outbox, "outbox");
        this.reloj = Requisitos.noNulo(reloj, "reloj");
    }

    @Override
    public void redactar(RedactarReceta.Comando comando) {
        Requisitos.noNulo(comando, "comando");
        unidadDeTrabajo.ejecutarSinResultado(() -> {
            if (recetas.buscar(comando.recetaId()).isEmpty()) {
                recetas.guardar(Receta.redactar(comando.recetaId(), comando.episodioId(), comando.prescriptorId()));
            }
        });
    }

    @Override
    public void agregar(AgregarMedicamento.Comando comando) {
        Requisitos.noNulo(comando, "comando");
        Medicamento medicamento = new Medicamento(comando.nombre(), comando.dosis(), comando.indicacion());
        unidadDeTrabajo.ejecutarSinResultado(() -> {
            Receta receta = cargar(comando.recetaId());
            receta.agregarMedicamento(medicamento);
            recetas.guardar(receta);
        });
    }

    @Override
    public void firmar(FirmarReceta.Comando comando) {
        Requisitos.noNulo(comando, "comando");
        unidadDeTrabajo.ejecutarSinResultado(() -> {
            Receta receta = cargar(comando.recetaId());
            receta.firmar(new FirmaDigital(comando.firmanteId(), reloj.instant(), comando.valorFirma()));
            recetas.guardar(receta);
            outbox.registrar(receta);
        });
    }

    @Override
    public void procesar(EventoEntrante entrante) {
        Requisitos.noNulo(entrante, "entrante");
        if (!(entrante.evento() instanceof RecetaFirmada firmada)) {
            return;
        }
        unidadDeTrabajo.ejecutarSinResultado(() -> {
            if (!inbox.registrarSiEsNuevo(CONSUMIDOR, entrante.eventoId())) {
                return;
            }
            Receta receta = cargar(firmada.recetaId());
            if (receta.enviadaAFarmaciaEn().isPresent()) {
                return;
            }
            farmacia.enviar(receta);
            receta.marcarEnviadaAFarmacia(reloj.instant());
            recetas.guardar(receta);
        });
    }

    private Receta cargar(IdClinico recetaId) {
        Requisitos.noNulo(recetaId, "recetaId");
        return recetas.buscar(recetaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("la receta " + recetaId));
    }
}
