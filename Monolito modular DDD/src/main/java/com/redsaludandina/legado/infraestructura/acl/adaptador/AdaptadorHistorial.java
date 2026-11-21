package com.redsaludandina.legado.infraestructura.acl.adaptador;

import com.redsaludandina.legado.aplicacion.puerto.salida.HistorialClinicoPort;
import com.redsaludandina.legado.dominio.modelo.HistorialClinico;
import com.redsaludandina.legado.dominio.modelo.MarcaFrescura;
import com.redsaludandina.legado.dominio.modelo.OrigenHistorial;
import com.redsaludandina.legado.infraestructura.acl.cliente.bd.ClienteBdLectura;
import com.redsaludandina.legado.infraestructura.acl.modelo.HistorialLegado;
import com.redsaludandina.legado.infraestructura.acl.readmodel.ReadModelHistorial;
import com.redsaludandina.legado.infraestructura.acl.traductor.TraductorCanonico;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;

/**
 * Fachada de la capa anticorrupción: la ÚNICA implementación de {@link HistorialClinicoPort}.
 *
 * <ol>
 *   <li>Si el read model tiene una copia vigente, la devuelve (p95 ≤ 2 s).</li>
 *   <li>Si no, consulta la base de solo lectura del legado (≤ 6 s), traduce al modelo canónico y
 *       refresca el read model con TTL.</li>
 *   <li>Si el legado no responde y hay una copia vencida, devuelve esa copia: su caducidad viaja
 *       visible en la respuesta («vigente = false»), nunca se presenta como fresca.</li>
 * </ol>
 */
public final class AdaptadorHistorial implements HistorialClinicoPort {

    private static final Logger LOG = LoggerFactory.getLogger(AdaptadorHistorial.class);

    private final ReadModelHistorial readModel;
    private final ClienteBdLectura clienteBd;
    private final TraductorCanonico traductor;
    private final Clock reloj;
    private final Duration ttl;

    public AdaptadorHistorial(ReadModelHistorial readModel, ClienteBdLectura clienteBd, TraductorCanonico traductor,
                              Clock reloj, Duration ttl) {
        this.readModel = readModel;
        this.clienteBd = clienteBd;
        this.traductor = traductor;
        this.reloj = reloj;
        this.ttl = ttl;
    }

    @Override
    public Optional<HistorialClinico> obtener(String pacienteId) {
        Instant ahora = reloj.instant();
        Optional<HistorialClinico> copia = readModel.buscar(pacienteId);
        if (copia.isPresent() && copia.get().estaVigente(ahora)) {
            return copia;
        }
        try {
            Optional<HistorialLegado> delLegado = clienteBd.obtener(traductor.codigoLegado(pacienteId));
            if (delLegado.isEmpty()) {
                return Optional.empty();
            }
            HistorialClinico fresco = traductor.traducir(delLegado.get(),
                    MarcaFrescura.conTtl(ahora, ttl, OrigenHistorial.LEGADO_DIRECTO));
            readModel.guardar(fresco);
            return Optional.of(fresco);
        } catch (DataAccessException legadoNoDisponible) {
            if (copia.isPresent()) {
                LOG.warn("Legado no disponible; se sirve la copia vencida de {}: {}", pacienteId,
                        legadoNoDisponible.getMessage());
                return copia;
            }
            throw legadoNoDisponible;
        }
    }
}
